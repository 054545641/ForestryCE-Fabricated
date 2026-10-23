package forestry.core.content.machines.tiles;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import com.google.common.base.Preconditions;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.IStillRecipe;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidRecipeFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerStill;
import forestry.core.content.machines.inventory.InventoryStill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import javax.annotation.Nullable;
import java.util.Objects;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileStill extends TilePowered implements WorldlyContainer, ILiquidTankTile {
	private static final int ENERGY_PER_RECIPE_TIME = 200;

	private final FilteredTank resourceTank;
	private final FilteredTank productTank;
	private final TankManager tankManager;

	@Nullable
	private IStillRecipe currentRecipe = null;
	private FluidVolume bufferedLiquid = FluidVolume.EMPTY;
	private record ProcessingState(IStillRecipe recipe, FluidVolume buffer) {}
	@Override protected Object captureProcessingState() { return new ProcessingState(this.currentRecipe, this.bufferedLiquid.copy()); }
	@Override protected void restoreProcessingState(Object state) {
		var previous = (ProcessingState) state;
		this.currentRecipe = previous.recipe();
		this.bufferedLiquid = previous.buffer();
	}

	public TileStill(BlockPos pos, BlockState state) {
		super(FactoryTiles.STILL.tileType(), pos, state, 1100, 80000);
		setInternalInventory(new InventoryStill(this));

		this.resourceTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY, true, true).setFilter(FluidRecipeFilter.STILL_INPUT);
		this.productTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY, false, true).setFilter(FluidRecipeFilter.STILL_OUTPUT);
		this.tankManager = new TankManager(this, this.resourceTank, this.productTank);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.tankManager.write(output);

		if (!this.bufferedLiquid.isEmpty()) {
			output.store("Buffer", FluidVolume.CODEC, this.bufferedLiquid);
		}
	
		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.tankManager.read(compoundNBT, registries);

		this.bufferedLiquid = input.read("Buffer", FluidVolume.CODEC).orElse(FluidVolume.EMPTY);
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
			FluidHelper.drainContainers(this.tankManager, this, InventoryStill.SLOT_CAN);

			FluidVolume fluidStack = this.productTank.getFluid();
			if (!fluidStack.isEmpty()) {
				FluidHelper.fillContainers(this.tankManager, this, InventoryStill.SLOT_RESOURCE, InventoryStill.SLOT_PRODUCT, fluidStack.getFluid(), true);
			}

			TankRenderInfo resourceTankInfo = this.getResourceTankInfo();
			TankRenderInfo productTankInfo = this.getProductTankInfo();

			int newLevelLeft = resourceTankInfo.getLevel().getLevelScaled(4);
			int newLevelRight = productTankInfo.getLevel().getLevelScaled(4);
			boolean update = false;

			if (state.hasProperty(BlockFactoryPlain.TANK_RESOURCE_LEVEL) &&
				state.getValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL) != newLevelLeft) {
				state = state.setValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL, newLevelLeft);
				update = true;
			}

			if (state.hasProperty(BlockFactoryPlain.TANK_PRODUCT_LEVEL) &&
				state.getValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL) != newLevelRight) {
				state = state.setValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL, newLevelRight);
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
		int cycles = this.currentRecipe.getCyclesPerUnit();
		FluidVolume output = this.currentRecipe.getOutput();

		FluidVolume product = output.copyWithAmount(output.getAmount() * cycles);
		try (var tx = beginWorkTransaction()) {
			FluidVolume input = this.currentRecipe.getInput();
			long amount = Math.multiplyExact(input.getAmount(), cycles);
			if (this.bufferedLiquid.isEmpty()) {
				if (this.resourceTank.extractInternal(input.variant(), amount, tx) != amount) return false;
			} else {
				if (!FluidVolume.isSameFluidSameComponents(this.bufferedLiquid, input) || this.bufferedLiquid.getAmount() < amount) return false;
				this.bufferedLiquid = this.bufferedLiquid.copyWithAmount(this.bufferedLiquid.getAmount() - amount);
			}
			if (!product.isEmpty() && this.productTank.insertInternal(product.variant(), product.getAmount(), tx) != product.getAmount()) return false;
			tx.commit();
		}

		return true;
	}

	private void checkRecipe() {
		FluidVolume recipeLiquid = !this.bufferedLiquid.isEmpty() ? this.bufferedLiquid : this.resourceTank.getFluid();

		if (this.currentRecipe == null || !this.currentRecipe.matches(recipeLiquid)) {
			Level level = Objects.requireNonNull(this.level);
			this.currentRecipe = RecipeUtils.getStillRecipe(RecipeUtils.getRecipeMap(level), recipeLiquid);

			int recipeTime = this.currentRecipe == null ? 0 : this.currentRecipe.getCyclesPerUnit();
			configureWork(this.currentRecipe == null ? "" : this.currentRecipe.getId().toString(), recipeTime, ENERGY_PER_RECIPE_TIME * recipeTime);
		}
	}

	@Override
	public boolean hasWork() {
		checkRecipe();

		boolean hasRecipe = this.currentRecipe != null;
		boolean hasTankSpace = true;
		boolean hasLiquidResource = true;

		if (hasRecipe) {
			FluidVolume output = this.currentRecipe.getOutput();
			FluidVolume fluidStack = output.copyWithAmount(output.getAmount() * this.currentRecipe.getCyclesPerUnit());
			hasTankSpace = FluidHelper.simulateFill(this.productTank.internal(), fluidStack) == fluidStack.getAmount();
			if (this.bufferedLiquid.isEmpty()) {
				int cycles = this.currentRecipe.getCyclesPerUnit();
				FluidVolume input = this.currentRecipe.getInput();
				long drainAmount = cycles * input.getAmount();
				FluidVolume drained = FluidHelper.simulateDrain(this.resourceTank, drainAmount);
				hasLiquidResource = !drained.isEmpty() && drained.getAmount() == drainAmount;
			} else {
				hasLiquidResource = this.bufferedLiquid.getAmount() >= this.currentRecipe.getCyclesPerUnit() * this.currentRecipe.getInput().getAmount();
			}
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasRecipe, ForestryError.NO_RECIPE);
		errorLogic.setCondition(!hasTankSpace, ForestryError.NO_SPACE_TANK);
		errorLogic.setCondition(!hasLiquidResource, ForestryError.NO_RESOURCE_LIQUID);

		return hasRecipe && hasLiquidResource && hasTankSpace;
	}

	@Override
	public TankRenderInfo getResourceTankInfo() {
		return new TankRenderInfo(this.resourceTank);
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return new TankRenderInfo(this.productTank);
	}


	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}


	public SlottedStorage<FluidVariant> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerStill(windowId, player.getInventory(), this);
	}

}
