package forestry.core.content.machines.tiles;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import forestry.core.platform.fluids.FluidHelper;
import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitSocketTypes;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.ISqueezerRecipe;
import forestry.core.engine.circuits.IMachineUpgradable;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerSqueezer;
import forestry.core.content.machines.inventory.InventorySqueezer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import javax.annotation.Nullable;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileSqueezer extends TilePowered implements ISocketable, WorldlyContainer, ILiquidTankTile, IMachineUpgradable {
	private static final int STEPS_PER_RECIPE_TIME = 1;
	private static final int ENERGY_PER_WORK_CYCLE = 2000;
	private static final int ENERGY_PER_RECIPE_TIME = ENERGY_PER_WORK_CYCLE / 10;

	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");

	private final TankManager tankManager;
	private final StandardTank productTank;
	private final InventorySqueezer inventory;
	@Nullable
	private ISqueezerRecipe currentRecipe;
	@Override protected Object captureProcessingState() { return this.currentRecipe; }
	@Override protected void restoreProcessingState(Object state) { this.currentRecipe = (ISqueezerRecipe) state; }

	public TileSqueezer(BlockPos pos, BlockState state) {
		super(FactoryTiles.SQUEEZER.tileType(), pos, state, 1100, Constants.MACHINE_MAX_ENERGY);
		this.inventory = new InventorySqueezer(this);
		setInternalInventory(this.inventory);
		this.productTank = new StandardTank(Constants.PROCESSOR_TANK_CAPACITY, false, true);
		this.tankManager = new TankManager(this, this.productTank);
	}

	/* LOADING & SAVING */

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
		this.tankManager.write(output);
		this.sockets.write(compoundNBT, registries);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
		this.tankManager.read(compoundNBT, registries);
		this.sockets.read(compoundNBT, registries);

		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				loadMachineUpgrade(chipset);
			}
		}
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
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		super.writeGuiData(data);
		this.sockets.writeData(data);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
		super.readGuiData(data);
		this.sockets.readData(data);
	}

	// / WORKING
	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (updateOnInterval(20)) {
			FluidVolume fluid = this.productTank.getFluid();
			if (!fluid.isEmpty()) {
				this.inventory.fillContainers(fluid, this.tankManager);
			}

			TankRenderInfo productTankInfo = this.getProductTankInfo();

			int newLevel = productTankInfo.getLevel().getLevelScaled(4);
			boolean update = false;

			if (state.hasProperty(BlockFactoryPlain.TANK_PRODUCT_LEVEL) &&
				state.getValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL) != newLevel) {
				state = state.setValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL, newLevel);
				update = true;
			}

			if (update) {
				level.setBlock(pos, state, BlockFactoryPlain.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	public boolean workCycle() {
		if (this.currentRecipe == null) return false;
		try (var tx = beginWorkTransaction()) {
			if (!this.inventory.removeResources(this.currentRecipe.getInputs())) return false;
			var random = workRandom();
			FluidVolume fluid = this.currentRecipe.getFluidOutput().createRandomFluidVolume(random);
			if (!fluid.isEmpty() && this.productTank.insertInternal(fluid.variant(), fluid.getAmount(), tx) != fluid.getAmount()) return false;
			float roll = random.nextFloat();
			if (!this.currentRecipe.getRemnants().isEmpty() && roll < this.currentRecipe.getRemnantsChance() * this.outputMultiplier
				&& !this.inventory.addRemnant(this.currentRecipe.getRemnants().copy(), true)) return false;
			tx.commit();
			return true;
		}
	}

	private boolean checkRecipe() {
		ISqueezerRecipe matchingRecipe = null;

		if (this.inventory.hasResources()) {
			List<ItemStack> resources = this.inventory.getResources();

			boolean containsSets = false;

			if (this.currentRecipe != null) {
				Container inventory = new InventoryMapper(this, InventorySqueezer.SLOT_RESOURCE_1, InventorySqueezer.SLOTS_RESOURCE_COUNT);
				containsSets = InventoryUtil.consumeIngredients(inventory, this.currentRecipe.getInputs(), null, false, false, false);
			}

			if (this.currentRecipe != null && containsSets) {
				matchingRecipe = this.currentRecipe;
			} else {
				matchingRecipe = RecipeUtils.getSqueezerRecipe(RecipeUtils.getRecipeMap(getLevel()), resources);
			}

			if (matchingRecipe == null) {
				for (ItemStack resource : resources) {
					if (matchingRecipe == null) {
						matchingRecipe = RecipeUtils.getSqueezerContainerRecipe(RecipeUtils.getRecipeMap(getLevel()), resource);
					}
				}
			}

			// A dynamic output (e.g. an addon tag product no loaded mod fills) resolves to an empty representative
			// stack; refuse the recipe rather than consuming the input for no fluid.
			if (matchingRecipe != null && matchingRecipe.getFluidOutput().createFluidVolume().isEmpty()) {
				matchingRecipe = null;
			}
		}

		if (this.currentRecipe != matchingRecipe) {
			this.currentRecipe = matchingRecipe;

			if (this.currentRecipe != null) {
				int recipeTime = this.currentRecipe.getProcessingTime();
				configureWork(this.currentRecipe.getId().toString(), recipeTime * STEPS_PER_RECIPE_TIME, recipeTime * ENERGY_PER_RECIPE_TIME);
			}
		}

		getErrorLogic().setCondition(this.currentRecipe == null, ForestryError.NO_RECIPE);
		return this.currentRecipe != null;
	}

	@Override
	public boolean hasWork() {
		checkRecipe();

		boolean hasResources = this.inventory.hasResources();
		boolean hasRecipe = true;
		boolean canFill = true;
		boolean canAdd = true;

		if (hasResources) {
			hasRecipe = this.currentRecipe != null;
			if (hasRecipe) {
				// Reserve worst-case space: createFluidVolume() is the MAX amount this product can emit, so the machine
				// only starts if the tank can hold it, even if a given cycle later produces less.
				FluidVolume resultFluid = this.currentRecipe.getFluidOutput().createFluidVolume();
				canFill = FluidHelper.simulateFill(this.productTank.internal(), resultFluid) == resultFluid.getAmount();

				if (!this.currentRecipe.getRemnants().isEmpty()) {
					canAdd = this.inventory.addRemnant(this.currentRecipe.getRemnants(), false);
				}
			}
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasResources, ForestryError.NO_RESOURCE);
		errorLogic.setCondition(!hasRecipe, ForestryError.NO_RECIPE);
		errorLogic.setCondition(!canFill, ForestryError.NO_SPACE_TANK);
		errorLogic.setCondition(!canAdd, ForestryError.NO_SPACE_INVENTORY);

		return hasResources && hasRecipe && canFill && canAdd;
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return new TankRenderInfo(this.productTank);
	}


	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	/* ISocketable */
	@Override
	public int getSocketCount() {
		return this.sockets.getContainerSize();
	}

	@Override
	public ItemStack getSocket(int slot) {
		return this.sockets.getItem(slot);
	}

	@Override
	public void setSocket(int slot, ItemStack stack) {
		if (stack.isEmpty() || IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(stack)) {
			// Dispose correctly of old chipsets
			if (!this.sockets.getItem(slot).isEmpty()) {
				if (IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(this.sockets.getItem(slot))) {
					ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(this.sockets.getItem(slot));
					if (chipset != null) {
						chipset.onRemoval(this);
					}
				}
			}

			this.sockets.setItem(slot, stack);
			// sockets has no tile link of its own
			setChanged();
			if (!stack.isEmpty()) {
				ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
				if (chipset != null) {
					chipset.onInsertion(this);
				}
			}
		}
	}

	@Override
	public Identifier getSocketType() {
		return ForestryCircuitSocketTypes.MACHINE;
	}

	public SlottedStorage<FluidVariant> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerSqueezer(windowId, inv, this);
	}
}
