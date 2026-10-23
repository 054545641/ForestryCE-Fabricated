package forestry.core.content.machines.tiles;

import forestry.core.platform.util.InventoryData;
import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitSocketTypes;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.ICentrifugeRecipe;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.config.Constants;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.tile.IItemStackDisplay;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerCentrifuge;
import forestry.core.content.machines.inventory.InventoryCentrifuge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileCentrifuge extends TilePowered implements ISocketable, WorldlyContainer, IItemStackDisplay {
	private static final int STEPS_PER_RECIPE_TIME = 1;
	private static final int ENERGY_PER_WORK_CYCLE = 3200;
	private static final int ENERGY_PER_RECIPE_TIME = ENERGY_PER_WORK_CYCLE / 20;

	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");
	private final ResultContainer craftPreviewInventory;
	private final ArrayDeque<ItemStack> pendingProducts = new ArrayDeque<>();
	@Nullable
	private ICentrifugeRecipe currentRecipe;
	private record ProcessingState(ICentrifugeRecipe recipe, java.util.List<ItemStack> pending, ItemStack preview) {}
	@Override protected Object captureProcessingState() {
		return new ProcessingState(this.currentRecipe, this.pendingProducts.stream().map(ItemStack::copy).toList(), this.craftPreviewInventory.getItem(0).copy());
	}
	@Override protected void restoreProcessingState(Object value) {
		var previous = (ProcessingState) value;
		this.currentRecipe = previous.recipe();
		this.pendingProducts.clear();
		this.pendingProducts.addAll(previous.pending());
		this.craftPreviewInventory.setItem(0, previous.preview());
	}
	@Override protected CycleResult completeWorkCycle() {
		int before = this.pendingProducts.size();
		if (workCycle()) return CycleResult.COMPLETE;
		return this.pendingProducts.size() < before ? CycleResult.PROGRESS : CycleResult.BLOCKED;
	}

	public TileCentrifuge(BlockPos pos, BlockState state) {
		super(FactoryTiles.CENTRIFUGE.tileType(), pos, state, 800, Constants.MACHINE_MAX_ENERGY);
		setInternalInventory(new InventoryCentrifuge(this));
		this.craftPreviewInventory = new ResultContainer();
	}

	/* LOADING & SAVING */

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		this.sockets.write(output);
		InventoryData.writeStacks(this.pendingProducts, "PendingProducts", output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compound = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

		InventoryData.replaceStacks(this.pendingProducts, compound.getListOrEmpty("PendingProducts"), registries);
		this.sockets.read(compound, registries);

		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				loadMachineUpgrade(chipset);
			}
		}
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

	@Override
	public boolean workCycle() {
		try (var transaction = beginWorkTransaction()) {
			if (!this.pendingProducts.isEmpty()) {
				if (!tryAddPending()) return false;
				transaction.commit();
				return this.pendingProducts.isEmpty();
			}
			if (this.currentRecipe == null) return false;
			ItemStack input = getItem(InventoryCentrifuge.SLOT_RESOURCE);
			if (input.isEmpty() || RecipeUtils.getCentrifugeRecipe(RecipeUtils.getRecipeMap(getLevel()), input) != this.currentRecipe) return false;
			Collection<ItemStack> products = this.currentRecipe.getProducts(workRandom(), this.outputMultiplier);
			for (ItemStack product : products) {
				if (!InventoryUtil.tryAddStack(this, product, InventoryCentrifuge.SLOT_PRODUCT_1, InventoryCentrifuge.SLOT_PRODUCT_COUNT, true)) return false;
			}
			if (getTransferHandler().getSlot(InventoryCentrifuge.SLOT_RESOURCE).extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(input), 1, transaction) != 1) return false;
			transaction.commit();
			return true;
		}
	}

	private void checkRecipe() {
		ItemStack resource = getItem(InventoryCentrifuge.SLOT_RESOURCE);
		ICentrifugeRecipe matchingRecipe = RecipeUtils.getCentrifugeRecipe(RecipeUtils.getRecipeMap(getLevel()), resource);

		if (this.currentRecipe != matchingRecipe) {
			this.currentRecipe = matchingRecipe;
			if (this.currentRecipe != null) {
				int recipeTime = this.currentRecipe.getProcessingTime();
				configureWork(this.currentRecipe.getId().toString(), recipeTime * STEPS_PER_RECIPE_TIME, recipeTime * ENERGY_PER_RECIPE_TIME);
			}
		}
	}

	private boolean tryAddPending() {
		if (this.pendingProducts.isEmpty()) {
			return false;
		}

		ItemStack next = this.pendingProducts.peekFirst();

		boolean added = InventoryUtil.tryAddStack(this, next, InventoryCentrifuge.SLOT_PRODUCT_1, InventoryCentrifuge.SLOT_PRODUCT_COUNT, true);

		if (added) {
			this.pendingProducts.removeFirst();
			if (this.pendingProducts.isEmpty()) {
				this.craftPreviewInventory.setItem(0, ItemStack.EMPTY);
			}
		}

		getErrorLogic().setCondition(!added, ForestryError.NO_SPACE_INVENTORY);
		return added;
	}

	@Override
	public boolean hasResourcesMin(float percentage) {
		IInventoryAdapter inventory = getInternalInventory();
		if (inventory.getItem(InventoryCentrifuge.SLOT_RESOURCE).isEmpty()) {
			return false;
		}

		return (float) inventory.getItem(InventoryCentrifuge.SLOT_RESOURCE).getCount() / (float) inventory.getItem(InventoryCentrifuge.SLOT_RESOURCE).getMaxStackSize() > percentage;
	}

	@Override
	public boolean hasWork() {
		if (!this.pendingProducts.isEmpty()) {
			configureWork("forestry:pending_centrifuge", 1, 0);
			return true;
		}
		checkRecipe();

		boolean hasResource = !getItem(InventoryCentrifuge.SLOT_RESOURCE).isEmpty();

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasResource, ForestryError.NO_RESOURCE);

		if (!hasResource || this.currentRecipe == null) return false;
		try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openNested(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getCurrentUnsafe())) {
			for (ItemStack product : this.currentRecipe.getProducts(previewWorkRandom(), this.outputMultiplier)) {
				if (!InventoryUtil.tryAddStack(this, product, InventoryCentrifuge.SLOT_PRODUCT_1, InventoryCentrifuge.SLOT_PRODUCT_COUNT, true)) {
					errorLogic.setCondition(true, ForestryError.NO_SPACE_INVENTORY);
					return false;
				}
			}
			errorLogic.setCondition(false, ForestryError.NO_SPACE_INVENTORY);
			return true;
		}
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

		if (!stack.isEmpty() && !IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(stack)) {
			return;
		}

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
		if (stack.isEmpty()) {
			return;
		}

		ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
		if (chipset != null) {
			chipset.onInsertion(this);
		}
	}

	@Override
	public Identifier getSocketType() {
		return ForestryCircuitSocketTypes.MACHINE;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerCentrifuge(windowId, player.getInventory(), this);
	}

	public Container getCraftPreviewInventory() {
		return this.craftPreviewInventory;
	}

	@Override
	public void handleItemStackForDisplay(ItemStack itemStack) {
		this.craftPreviewInventory.setItem(0, itemStack);
	}
}
