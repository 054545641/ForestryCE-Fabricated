package forestry.core.content.machines.tiles;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import java.util.Optional;

import forestry.core.platform.fluids.FluidHelper;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.api.core.machines.IFabricatorSmeltingRecipe;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidRecipeFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.platform.inventory.InventoryGhostCrafting;
import forestry.core.platform.inventory.watchers.ISlotPickupWatcher;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.core.platform.item.ICraftingPlan;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerFabricator;
import forestry.core.content.machines.inventory.InventoryFabricator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import forestry.api.core.fluids.FluidVolume;

import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileFabricator extends TilePowered implements ISlotPickupWatcher, ILiquidTankTile, WorldlyContainer {
	public static final int MAX_HEAT = 5000;

	private final InventoryAdapterTile craftingInventory;
	private final TankManager tankManager;
	private final FilteredTank moltenTank;
	private int heat = 0;
	private int meltingPoint = 0;
	private record ProcessingState(int heat, int meltingPoint) {}
	@Override protected Object captureProcessingState() { return new ProcessingState(this.heat, this.meltingPoint); }
	@Override protected void restoreProcessingState(Object value) {
		var previous = (ProcessingState) value;
		this.heat = previous.heat();
		this.meltingPoint = previous.meltingPoint();
	}

	public TileFabricator(BlockPos pos, BlockState state) {
		super(FactoryTiles.FABRICATOR.tileType(), pos, state, 1100, 3300);
		setEnergyPerWorkCycle(200);
        this.craftingInventory = new InventoryGhostCrafting<>(this, InventoryGhostCrafting.SLOT_CRAFTING_COUNT);
		setInternalInventory(new InventoryFabricator(this));

        this.moltenTank = new FilteredTank(8000, false, true).setFilter(FluidRecipeFilter.FABRICATOR_SMELTING_OUTPUT);

        this.tankManager = new TankManager(this, this.moltenTank);
	}

	/* SAVING & LOADING */

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compound = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		compound.putInt("Heat", this.heat);
		compound.putInt("MeltingPont", this.meltingPoint);
        this.tankManager.write(output);
        this.craftingInventory.write(output);

		ValueIOUtil.mergeInto(output, compound);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compound = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.heat = compound.getIntOr("Heat", 0);
		this.meltingPoint = compound.getIntOr("MeltingPont", 0);
        this.tankManager.read(compound, registries);
        this.craftingInventory.read(compound, registries);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
        this.tankManager.writeData(data);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
        this.tankManager.readData(data);
	}

	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		super.writeGuiData(data);
		data.writeVarInt(this.heat);
		data.writeVarInt(this.meltingPoint);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
		super.readGuiData(data);
		this.heat = data.readVarInt();
		this.meltingPoint = data.readVarInt();
	}

	/* UPDATING */
	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		try (var transaction = beginWorkTransaction()) {
			if (!this.moltenTank.isFull()) {
				trySmelting();
			}

			if (!this.moltenTank.isEmpty()) {
				// Remove smelt if we have gone below melting point
				if (this.heat < getMeltingPoint() - 100) {
	                FluidHelper.drain(this.moltenTank, forestry.api.core.fluids.FluidUnits.fromMilliBuckets(5));
				}
			}

			if (this.heat > 2500) {
				this.heat -= 2;
			} else if (this.heat > 0) {
				this.heat--;
			}
			transaction.commit();
		}
	}

	private void trySmelting() {
		IInventoryAdapter inventory = getInternalInventory();

		ItemStack smeltResource = inventory.getItem(InventoryFabricator.SLOT_METAL);
		if (smeltResource.isEmpty()) {
			return;
		}

		IFabricatorSmeltingRecipe smelt = RecipeUtils.getFabricatorMeltingRecipe(RecipeUtils.getRecipeMap(this.level), smeltResource);
		if (smelt == null || smelt.getMeltingPoint() > this.heat) {
			return;
		}

		FluidVolume smeltFluid = smelt.getResultFluid();
		try (var tx = beginWorkTransaction()) {
			if (this.moltenTank.insertInternal(smeltFluid.variant(), smeltFluid.getAmount(), tx) != smeltFluid.getAmount()) return;
			ItemStack metal = getItem(InventoryFabricator.SLOT_METAL);
			if (metal.isEmpty() || getTransferHandler().getSlot(InventoryFabricator.SLOT_METAL).extract(ItemVariant.of(metal), 1, tx) != 1) return;
			this.meltingPoint = smelt.getMeltingPoint();
			tx.commit();
		}
	}

	@Override
	public boolean workCycle() {
		try (var transaction = beginWorkTransaction()) {
			this.heat = Math.min(MAX_HEAT, this.heat + 100);
			craftResult();
			transaction.commit();
			return true;
		}
	}

	@Nullable
	private IFabricatorRecipe getRecipe() {
		IInventoryAdapter inventory = getInternalInventory();
		ItemStack plan = inventory.getItem(InventoryFabricator.SLOT_PLAN);
		FluidVolume liquid = this.moltenTank.getFluid();
		IFabricatorRecipe recipe = RecipeUtils.getFabricatorRecipe(RecipeUtils.getRecipeMap(this.level), this.level, liquid, plan, this.craftingInventory);
		if (!liquid.isEmpty() && recipe != null && !(FluidVolume.isSameFluidSameComponents(liquid, recipe.getResultFluid()) && liquid.getAmount() >= recipe.getResultFluid().getAmount())) {
			return null;
		}
		return recipe;
	}

	public ItemStack getResult(@Nullable IFabricatorRecipe myRecipe) {
		if (myRecipe == null) {
			return ItemStack.EMPTY;
		}

		return myRecipe.getResultItem(this.level.registryAccess()).copy();
	}

	/* ISlotPickupWatcher */
	@Override
	public void onTake(int slotIndex, Player player) {
		if (slotIndex == InventoryFabricator.SLOT_RESULT) {
			removeItem(InventoryFabricator.SLOT_RESULT, 1);
		}
	}

	private void craftResult() {
		IFabricatorRecipe recipe = getRecipe();
		ItemStack result = getResult(recipe);
		if (recipe == null || result.isEmpty() || !getItem(InventoryFabricator.SLOT_RESULT).isEmpty()) return;
		try (var tx = beginWorkTransaction()) {
			FluidVolume liquid = recipe.getResultFluid();
			if (!liquid.isEmpty() && this.moltenTank.extractInternal(liquid.variant(), liquid.getAmount(), tx) != liquid.getAmount()) return;
			if (!removeFromInventory(recipe, true)) return;
			ItemStack plan = getItem(InventoryFabricator.SLOT_PLAN).copy();
			if (!plan.isEmpty() && plan.getItem() instanceof ICraftingPlan craftingPlan) {
				ItemStack used = craftingPlan.planUsed(plan.copy(), result.copy());
				if (getTransferHandler().getSlot(InventoryFabricator.SLOT_PLAN).extract(ItemVariant.of(plan), plan.getCount(), tx) != plan.getCount()) return;
				if (!used.isEmpty() && getTransferHandler().getSlot(InventoryFabricator.SLOT_PLAN).insert(ItemVariant.of(used), used.getCount(), tx) != used.getCount()) return;
			}
			if (!InventoryUtil.tryAddStack(this, result, InventoryFabricator.SLOT_RESULT, 1, true)) return;
			tx.commit();
		}
	}

	private boolean removeFromInventory(IFabricatorRecipe recipe, boolean doRemove) {
		Container inventory = new InventoryMapper(this, InventoryFabricator.SLOT_INVENTORY_1, InventoryFabricator.SLOT_INVENTORY_COUNT);
		return InventoryUtil.consumeIngredients(inventory, recipe.getCraftingGridRecipe().getIngredients().stream().flatMap(Optional::stream).toList(), null, true, false, doRemove);
	}

	@Override
	public boolean hasWork() {
		boolean hasRecipe = true;
		boolean hasLiquidResources = true;
		boolean hasResources = true;

		ItemStack plan = getItem(InventoryFabricator.SLOT_PLAN);
		IFabricatorRecipe recipe = RecipeUtils.getFabricatorRecipe(RecipeUtils.getRecipeMap(this.level), this.level, this.moltenTank.getFluid(), plan, this.craftingInventory);
		if (recipe != null) {
			hasResources = removeFromInventory(recipe, false);
			FluidVolume toDrain = recipe.getResultFluid();
			FluidVolume drained = FluidHelper.simulateDrain(this.moltenTank.internal(), toDrain);
			hasLiquidResources = !drained.isEmpty() && FluidVolume.matches(drained, toDrain);
		} else {
			hasRecipe = RecipeUtils.getFabricatorMeltingRecipe(RecipeUtils.getRecipeMap(this.level), getItem(InventoryFabricator.SLOT_METAL)) != null;
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasRecipe, ForestryError.NO_RECIPE);
		errorLogic.setCondition(!hasLiquidResources, ForestryError.NO_RESOURCE_LIQUID);
		errorLogic.setCondition(!hasResources, ForestryError.NO_RESOURCE_INVENTORY);

		return hasRecipe;
	}

	public int getHeatScaled(int i) {
		return this.heat * i / MAX_HEAT;
	}

	public int getHeat() {
		return this.heat;
	}

	public int getMeltingPoint() {
		if (!this.getItem(InventoryFabricator.SLOT_METAL).isEmpty()) {
			IFabricatorSmeltingRecipe meltingRecipe = RecipeUtils.getFabricatorMeltingRecipe(RecipeUtils.getRecipeMap(getLevel()), this.getItem(InventoryFabricator.SLOT_METAL));
			return meltingRecipe == null ? 0 : meltingRecipe.getMeltingPoint();
		} else if (this.moltenTank.getFluidAmount() > 0) {
			return this.meltingPoint;
		}

		return 0;
	}

	public int getMeltingPointScaled(int i) {
		int meltingPoint = getMeltingPoint();

		if (meltingPoint <= 0) {
			return 0;
		} else {
			return meltingPoint * i / MAX_HEAT;
		}
	}

	/* SMP */
	public void getGUINetworkData(int i, int j) {
		if (i == 0) {
            this.heat = j;
		} else if (i == 1) {
            this.meltingPoint = j;
		}
	}

	public void sendGUINetworkData(AbstractContainerMenu container, ContainerListener iCrafting) {
		iCrafting.dataChanged(container, 0, this.heat);
		iCrafting.dataChanged(container, 1, getMeltingPoint());
	}

	/**
	 * @return Inaccessible crafting inventory for the craft grid.
	 */
	public InventoryAdapter getCraftingInventory() {
		return this.craftingInventory;
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
		return new ContainerFabricator(windowId, player.getInventory(), this);
	}
}
