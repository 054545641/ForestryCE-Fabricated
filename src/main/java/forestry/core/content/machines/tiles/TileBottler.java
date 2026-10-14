package forestry.core.content.machines.tiles;

import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.capabilities.Capabilities;

import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidHelper.FillStatus;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.fluids.TankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.inventory.watchers.ISlotPickupWatcher;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerBottler;
import forestry.core.content.machines.inventory.InventoryBottler;
import forestry.core.content.machines.recipes.BottlerRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileBottler extends TilePowered implements WorldlyContainer, ILiquidTankTile, ISlotPickupWatcher {
	private static final int STEPS_PER_RECIPE_TIME = 5;
	private static final int ENERGY_PER_RECIPE_TIME = 1000;

	private final StandardTank resourceTank;
	private final TankManager tankManager;

	private boolean dumpingFluid = false;
	@Nullable
	private BottlerRecipe currentRecipe;
	@Override protected Object captureProcessingState() { return this.currentRecipe; }
	@Override protected void restoreProcessingState(Object state) { this.currentRecipe = (BottlerRecipe) state; }

	private void configureRecipe(float fillTime, boolean emptying) {
		String key = this.currentRecipe.fillRecipe + "/"
			+ forestry.core.platform.util.InventoryData.writeStack(this.currentRecipe.inputStack, this.level.registryAccess()) + "/"
			+ forestry.core.platform.util.InventoryData.writeStack(this.currentRecipe.outputStack, this.level.registryAccess());
		configureWork(key, Math.round(fillTime * STEPS_PER_RECIPE_TIME), emptying ? 0 : Math.round(fillTime * ENERGY_PER_RECIPE_TIME));
	}
	public boolean isFillRecipe;

	public TileBottler(BlockPos pos, BlockState state) {
		super(FactoryTiles.BOTTLER.tileType(), pos, state, 1100, 40000);

		setInternalInventory(new InventoryBottler(this));

        this.resourceTank = new StandardTank(Constants.PROCESSOR_TANK_CAPACITY);
        this.tankManager = new TankManager(this, this.resourceTank);

	}

	/* SAVING & LOADING */

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compound = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.tankManager.write(output);

		ValueIOUtil.mergeInto(output, compound);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compound = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.tankManager.read(compound, registries);
		this.currentRecipe = null; // Rebuild after the level is attached; preserve saved work progress.
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
        this.tankManager.writeData(data);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
        this.tankManager.readData(data);
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (updateOnInterval(20)) {
			stageContainer(InventoryBottler.SLOT_INPUT_FULL_CONTAINER, InventoryBottler.SLOT_EMPTYING_PROCESSING);
			stageContainer(InventoryBottler.SLOT_INPUT_EMPTY_CONTAINER, InventoryBottler.SLOT_FILLING_PROCESSING);

			TankRenderInfo resourceTankInfo = this.getResourceTankInfo();

			int newLevel = resourceTankInfo.getLevel().getLevelScaled(4);
			boolean update = false;

			if (state.hasProperty(BlockFactoryPlain.TANK_RESOURCE_LEVEL) &&
				state.getValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL) != newLevel) {
				state = state.setValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL, newLevel);
				update = true;
			}

			if (update) {
				level.setBlock(pos, state, BlockFactoryPlain.UPDATE_CLIENTS);
			}
		}

		if (this.dumpingFluid || updateOnInterval(20)) this.dumpingFluid = dumpFluid();
	}

	private void stageContainer(int input, int processing) {
		if (!getItem(processing).isEmpty() || getItem(input).isEmpty()) return;
		var resource = ItemResource.of(getItem(input));
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			var inventory = getTransferHandler();
			if (inventory.extract(input, resource, 1, transaction) != 1 || inventory.insert(processing, resource, 1, transaction) != 1) return;
			transaction.commit();
		}
	}

	private boolean dumpFluid() {
		if (this.resourceTank.isEmpty()) return false;
		// Capacity and accepted fluid can change without a neighbour block change.
		for (Direction facing : Direction.values()) {
			var destination = this.level.getCapability(Capabilities.Fluid.BLOCK, this.worldPosition.relative(facing), facing.getOpposite());
			if (destination != null && ResourceHandlerUtil.move(this.tankManager, destination, resource -> true,
				FluidType.BUCKET_VOLUME / 20, Transaction.getCurrentOpenedTransaction()) > 0) return true;
		}
		return false;
	}

	@Override
	public boolean workCycle() {
		try (var transaction = beginWorkTransaction()) {
			FluidHelper.FillStatus status;
			if (this.currentRecipe != null) {
				if (this.currentRecipe.fillRecipe) {
					status = FluidHelper.fillContainers(this.tankManager, this, InventoryBottler.SLOT_FILLING_PROCESSING, InventoryBottler.SLOT_OUTPUT_FULL_CONTAINER, this.currentRecipe.fluid.getFluid(), true);
				} else {
					status = FluidHelper.drainContainers(this.tankManager, this, InventoryBottler.SLOT_EMPTYING_PROCESSING, InventoryBottler.SLOT_OUTPUT_EMPTY_CONTAINER, true);
				}
			} else {
				return false;
			}

			if (status == FluidHelper.FillStatus.SUCCESS) {
	            this.currentRecipe = null;
				transaction.commit();
				return true;
			}
			return false;
		}
	}


	private void checkFillRecipe() {
		ItemStack emptyCan = getItem(InventoryBottler.SLOT_FILLING_PROCESSING);
		if (!emptyCan.isEmpty()) {
			FluidStack resource = this.resourceTank.getFluid();
			if (resource.isEmpty()) {
				return;
			}
			//Fill Container
			if (this.currentRecipe == null || !this.currentRecipe.matchEmpty(emptyCan, resource)) {
                this.currentRecipe = BottlerRecipe.createFillingRecipe(resource, emptyCan);
				if (this.currentRecipe != null) {
					float viscosityMultiplier = resource.getFluid().getFluidType().getViscosity(resource) / 1000.0f;
					viscosityMultiplier = (viscosityMultiplier - 1f) / 20f + 1f; // scale down the effect

					int fillAmount = Math.min(this.currentRecipe.fluid.getAmount(), resource.getAmount());
					float fillTime = fillAmount / (float) FluidType.BUCKET_VOLUME;
					fillTime *= viscosityMultiplier;

					configureRecipe(fillTime, false);
				}
			}
		}
	}

	private void checkEmptyRecipe() {
		ItemStack filledCan = getItem(InventoryBottler.SLOT_EMPTYING_PROCESSING);
		if (!filledCan.isEmpty()) {
			//Empty Container
			if (this.currentRecipe == null || !this.currentRecipe.matchFilled(filledCan)) {
                this.currentRecipe = BottlerRecipe.createEmptyingRecipe(filledCan);
				if (this.currentRecipe != null) {
					FluidStack resource = this.currentRecipe.fluid;
					float viscosityMultiplier = resource.getFluid().getFluidType().getViscosity(resource) / 1000.0f;
					viscosityMultiplier = (viscosityMultiplier - 1f) / 20f + 1f; // scale down the effect

					int fillAmount = Math.min(this.currentRecipe.fluid.getAmount(), resource.getAmount());
					float fillTime = fillAmount / (float) FluidType.BUCKET_VOLUME;
					fillTime *= viscosityMultiplier;

					configureRecipe(fillTime, true);
				}
			}
		}
	}

	@Override
	public void onTake(int slotIndex, Player player) {
		if (slotIndex == InventoryBottler.SLOT_EMPTYING_PROCESSING) {
			if (this.currentRecipe != null && !this.currentRecipe.fillRecipe) {
                this.currentRecipe = null;
				setStepsPerWorkCycle(0);
			}
		} else if (slotIndex == InventoryBottler.SLOT_FILLING_PROCESSING) {
			if (this.currentRecipe != null && this.currentRecipe.fillRecipe) {
                this.currentRecipe = null;
				setStepsPerWorkCycle(0);
			}
		}
	}

	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		super.writeGuiData(data);
		if (this.currentRecipe == null) {
			data.writeBoolean(false);
		} else {
			data.writeBoolean(this.currentRecipe.fillRecipe);
		}
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
		super.readGuiData(data);
        this.isFillRecipe = data.readBoolean();
	}

	@Override
	public boolean hasResourcesMin(float percentage) {
		IInventoryAdapter inventory = getInternalInventory();
		ItemStack emptyCan = inventory.getItem(InventoryBottler.SLOT_FILLING_PROCESSING);
		if (emptyCan.isEmpty()) {
			return false;
		}

		return (float) emptyCan.getCount() / (float) emptyCan.getMaxStackSize() > percentage;
	}

	@Override
	public boolean hasWork() {
		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.clearErrors();
		// Select an operation before configuring its paid progress. A blocked emptying
		// attempt must not reset an in-progress fill on every work step.
		FillStatus empty = FluidHelper.drainContainers(this.tankManager, this, InventoryBottler.SLOT_EMPTYING_PROCESSING,
			InventoryBottler.SLOT_OUTPUT_EMPTY_CONTAINER, false);
		if (empty == FillStatus.SUCCESS) {
			checkEmptyRecipe();
			return this.currentRecipe != null && !this.currentRecipe.fillRecipe;
		}
		FillStatus fill = this.resourceTank.isEmpty() ? FillStatus.NO_FLUID : FluidHelper.fillContainers(this.tankManager, this,
			InventoryBottler.SLOT_FILLING_PROCESSING, InventoryBottler.SLOT_OUTPUT_FULL_CONTAINER, this.resourceTank.getFluid().getFluid(), false);
		if (fill == FillStatus.SUCCESS) {
			checkFillRecipe();
			return this.currentRecipe != null && this.currentRecipe.fillRecipe;
		}
		errorLogic.setCondition(fill == FillStatus.NO_FLUID, ForestryError.NO_RESOURCE_LIQUID);
		errorLogic.setCondition(fill == FillStatus.NO_SPACE || empty == FillStatus.NO_SPACE, ForestryError.NO_SPACE_INVENTORY);
		errorLogic.setCondition(empty == FillStatus.NO_SPACE_FLUID, ForestryError.NO_SPACE_TANK);
		return false;
	}

	@Override
	public TankRenderInfo getResourceTankInfo() {
		return new TankRenderInfo(this.resourceTank);
	}

	/* ILIQUIDCONTAINER */

	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerBottler(windowId, player.getInventory(), this);
	}
}
