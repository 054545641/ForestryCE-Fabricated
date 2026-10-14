package forestry.core.content.machines.tiles;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidRecipeFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.platform.inventory.InventoryGhostCrafting;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.IItemStackDisplay;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerCarpenter;
import forestry.core.content.machines.inventory.InventoryCarpenter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileCarpenter extends TilePowered implements WorldlyContainer, ILiquidTankTile, IItemStackDisplay {
	private static final int STEPS_PER_RECIPE_TIME = 1;
	private static final int ENERGY_PER_WORK_CYCLE = 2040;
	private static final int ENERGY_PER_RECIPE_TIME = ENERGY_PER_WORK_CYCLE / 10;

	private final FilteredTank resourceTank;
	private final TankManager tankManager;
	private final InventoryAdapterTile craftingInventory;
	private final ResultContainer craftPreviewInventory;

	@Nullable
	private ICarpenterRecipe currentRecipe;
	private record ProcessingState(ICarpenterRecipe recipe, ItemStack preview) {}
	@Override protected Object captureProcessingState() { return new ProcessingState(this.currentRecipe, this.craftPreviewInventory.getItem(0).copy()); }
	@Override protected void restoreProcessingState(Object state) {
		var previous = (ProcessingState) state;
		this.currentRecipe = previous.recipe();
		this.craftPreviewInventory.setItem(0, previous.preview());
	}

	private ItemStack getBoxStack() {
		return getInternalInventory().getItem(InventoryCarpenter.SLOT_BOX);
	}

	public TileCarpenter(BlockPos pos, BlockState state) {
		super(FactoryTiles.CARPENTER.tileType(), pos, state, 1100, Constants.MACHINE_MAX_ENERGY);
		setEnergyPerWorkCycle(ENERGY_PER_WORK_CYCLE);
        this.resourceTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY).setFilter(FluidRecipeFilter.CARPENTER_INPUT);

        this.craftingInventory = new InventoryGhostCrafting<>(this, 10);
        this.craftPreviewInventory = new ResultContainer();
		setInternalInventory(new InventoryCarpenter(this));

        this.tankManager = new TankManager(this, this.resourceTank);
	}

	/* LOADING & SAVING */

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

        this.tankManager.write(output);
        this.craftingInventory.write(compoundNBT, registries);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.tankManager.read(compoundNBT, registries);
        this.craftingInventory.read(compoundNBT, registries);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
        this.tankManager.writeData(data);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
        this.tankManager.readData(data);
	}

	public void checkRecipe(RegistryAccess registryAccess) {
		if (this.level.isClientSide()) {
			return;
		}

		if (this.currentRecipe == null || !this.currentRecipe.matches(this.resourceTank.getFluid(), getBoxStack(), this.craftingInventory, this.level)) {
			ICarpenterRecipe recipe = RecipeUtils.getCarpenterRecipe(RecipeUtils.getRecipeMap(this.level), this.resourceTank.getFluid(), getBoxStack(), this.craftingInventory, this.level);
			this.currentRecipe = recipe;

			if (recipe != null) {
				int recipeTime = this.currentRecipe.getPackagingTime();
				configureWork(recipe.getId().toString(), recipeTime * STEPS_PER_RECIPE_TIME, recipeTime * ENERGY_PER_RECIPE_TIME);

				ItemStack craftingResult = this.currentRecipe.getResultItem(registryAccess);
                this.craftPreviewInventory.setItem(0, craftingResult);
			} else {
                this.craftPreviewInventory.setItem(0, ItemStack.EMPTY);
			}
		}
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (updateOnInterval(20)) {
			FluidHelper.drainContainers(this.tankManager, this, InventoryCarpenter.SLOT_CAN_INPUT);

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
	}

	@Override
	public boolean workCycle() {
		if (this.currentRecipe == null) return false;
		try (var tx = beginWorkTransaction()) {
			var fluid = this.currentRecipe.getInputFluid();
			if (!fluid.isEmpty() && this.resourceTank.extractInternal(FluidResource.of(fluid), fluid.getAmount(), tx) != fluid.getAmount()) return false;
			if (this.currentRecipe.getBox().isPresent()) {
				ItemStack box = getBoxStack();
				if (!this.currentRecipe.getBox().get().test(box) || getTransferHandler().extract(InventoryCarpenter.SLOT_BOX, ItemResource.of(box), 1, tx) != 1) return false;
			}
			Container ingredients = new InventoryMapper(getInternalInventory(), InventoryCarpenter.SLOT_INVENTORY_1, InventoryCarpenter.SLOT_INVENTORY_COUNT);
			if (!InventoryUtil.consumeIngredients(ingredients, this.currentRecipe.getCraftingGridRecipe().placementInfo().ingredients(), null, true, false, true)) return false;
			if (!InventoryUtil.tryAddStack(this, this.currentRecipe.getResultItem(this.level.registryAccess()), InventoryCarpenter.SLOT_PRODUCT, InventoryCarpenter.SLOT_PRODUCT_COUNT, true)) return false;
			tx.commit();
			return true;
		}
	}

	private boolean hasLiquidResources() {
		if (this.currentRecipe == null) {
			return true;
		}

		FluidStack fluid = this.currentRecipe.getInputFluid();
		if (!fluid.isEmpty()) {
			FluidStack drained = FluidHelper.simulateDrain(this.resourceTank.internal(), fluid);
			if (!FluidStack.matches(fluid, drained)) {
				return false;
			}
		}

		return true;
	}

	private boolean hasItemResources() {
		if (this.currentRecipe == null) {
			return true;
		}

		if (!this.currentRecipe.getBox().isEmpty()) {
			ItemStack box = getItem(InventoryCarpenter.SLOT_BOX);
			if (!this.currentRecipe.getBox().get().test(box)) {
				return false;
			}
		}

		Container inventory = new InventoryMapper(getInternalInventory(), InventoryCarpenter.SLOT_INVENTORY_1, InventoryCarpenter.SLOT_INVENTORY_COUNT);
		return InventoryUtil.consumeIngredients(inventory, this.currentRecipe.getCraftingGridRecipe().placementInfo().ingredients(), null, true, false, false);
	}

	/* STATE INFORMATION */
	@Override
	public boolean hasWork() {
		checkRecipe(this.level.registryAccess());

		boolean hasRecipe = this.currentRecipe != null;
		boolean hasLiquidResources = true;
		boolean hasItemResources = true;
		boolean canAdd = true;

		if (hasRecipe) {
			hasLiquidResources = hasLiquidResources();
			hasItemResources = hasItemResources();

			ItemStack pendingProduct = this.currentRecipe.getResultItem(this.level.registryAccess());
			canAdd = InventoryUtil.tryAddStack(this, pendingProduct, InventoryCarpenter.SLOT_PRODUCT, InventoryCarpenter.SLOT_PRODUCT_COUNT, true, false);
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasRecipe, ForestryError.NO_RECIPE);
		errorLogic.setCondition(!hasLiquidResources, ForestryError.NO_RESOURCE_LIQUID);
		errorLogic.setCondition(!hasItemResources, ForestryError.NO_RESOURCE_INVENTORY);
		errorLogic.setCondition(!canAdd, ForestryError.NO_SPACE_INVENTORY);

		return hasRecipe && hasItemResources && hasLiquidResources && canAdd;
	}

	@Override
	public TankRenderInfo getResourceTankInfo() {
		return new TankRenderInfo(this.resourceTank);
	}

	/**
	 * @return Inaccessible crafting inventory for the craft grid.
	 */
	public Container getCraftingInventory() {
		return this.craftingInventory;
	}

	public Container getCraftPreviewInventory() {
		return this.craftPreviewInventory;
	}

	@Override
	public void handleItemStackForDisplay(ItemStack itemStack) {
        this.craftPreviewInventory.setItem(0, itemStack);
	}


	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerCarpenter(windowId, player.getInventory(), this);
	}
}
