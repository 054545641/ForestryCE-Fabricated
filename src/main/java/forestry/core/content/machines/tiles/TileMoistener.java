package forestry.core.content.machines.tiles;

import forestry.core.platform.util.InventoryData;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.fuels.MoistenerFuel;
import forestry.api.core.machines.IMoistenerRecipe;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidTagFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.IRenderableTile;
import forestry.core.platform.tile.TileBase;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerMoistener;
import forestry.core.content.machines.inventory.InventoryMoistener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import forestry.api.ForestryDataMaps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileMoistener extends TileBase implements WorldlyContainer, ILiquidTankTile, IRenderableTile {
	private final FilteredTank resourceTank;
	private final TankManager tankManager;
	@Nullable
	private IMoistenerRecipe currentRecipe;

	private int burnTime = 0;
	private int totalTime = 0;
	private int productionTime = 0;
	private int timePerItem = 0;
	@Nullable
	private ItemStack currentProduct;
	@Nullable
	private ItemStack pendingProduct;

	private String activeRecipe = "";
	private record WorkState(int burn, int total, int production, int time, String id, IMoistenerRecipe recipe, ItemStack current, ItemStack pending) {}
	private final SnapshotJournal<WorkState> workJournal = new SnapshotJournal<>() {
		@Override protected WorkState createSnapshot() {
			return new WorkState(burnTime, totalTime, productionTime, timePerItem, activeRecipe, currentRecipe,
				currentProduct == null ? null : currentProduct.copy(), pendingProduct == null ? null : pendingProduct.copy());
		}
		@Override protected void revertToSnapshot(WorkState previous) {
			burnTime = previous.burn(); totalTime = previous.total(); productionTime = previous.production(); timePerItem = previous.time();
			activeRecipe = previous.id(); currentRecipe = previous.recipe(); currentProduct = previous.current(); pendingProduct = previous.pending();
		}
		@Override protected void onRootCommit(WorkState previous) { setChanged(); }
	};

	public TileMoistener(BlockPos pos, BlockState state) {
		super(FactoryTiles.MOISTENER.tileType(), pos, state);
		setInternalInventory(new InventoryMoistener(this));
        this.resourceTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY).setFilter(FluidTagFilter.WATER);
        this.tankManager = new TankManager(this, this.resourceTank);
	}

	/* LOADING & SAVING */
	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		compoundNBT.putInt("BurnTime", this.burnTime);
		compoundNBT.putInt("TotalTime", this.totalTime);
		compoundNBT.putInt("ProductionTime", this.productionTime);
		compoundNBT.putInt("TimePerItem", this.timePerItem);
		compoundNBT.putString("ActiveRecipe", this.activeRecipe);

        this.tankManager.write(output);

		output.storeNullable("PendingProduct", ItemStack.OPTIONAL_CODEC, this.pendingProduct);
		output.storeNullable("CurrentProduct", ItemStack.OPTIONAL_CODEC, this.currentProduct);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.burnTime = compoundNBT.getIntOr("BurnTime", 0);
        this.totalTime = compoundNBT.getIntOr("TotalTime", 0);
        this.productionTime = compoundNBT.getIntOr("ProductionTime", 0);
		this.timePerItem = compoundNBT.getIntOr("TimePerItem", 0);
		this.activeRecipe = compoundNBT.getStringOr("ActiveRecipe", "");

        this.tankManager.read(compoundNBT, registries);

		this.pendingProduct = InventoryData.readNullableStack(compoundNBT, "PendingProduct", registries);
		this.currentProduct = InventoryData.readNullableStack(compoundNBT, "CurrentProduct", registries);

		this.currentRecipe = null;
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

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {

		if (updateOnInterval(20)) {
			// Check if we have suitable water container waiting in the item slot
			FluidHelper.drainContainers(this.tankManager, this, InventoryMoistener.SLOT_PRODUCT);

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

		// Let's get to work
		//TODO correct method?
		int lightvalue = level.getLightEmission(getBlockPos().above());

		IErrorLogic errorLogic = getErrorLogic();

		// Not working in broad daylight
		boolean gloomy = lightvalue <= 11;
		if (errorLogic.setCondition(!gloomy, ForestryError.NOT_DARK)) {
			return;
		}

		// The darker, the better
		int speed;
		if (lightvalue >= 9) {
			speed = 1;
		} else if (lightvalue >= 7) {
			speed = 2;
		} else if (lightvalue >= 5) {
			speed = 3;
		} else {
			speed = 4;
		}

		processWorkStep(speed);
	}

	/** One moistening step, including fuel rotation and pending output, is transactional. */
	public void processWorkStep(int speed) {
		if (speed <= 0) throw new IllegalArgumentException("Non-positive moistener speed");
		IErrorLogic errorLogic = getErrorLogic();
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			this.workJournal.updateSnapshots(transaction);
			// Already running
			if (this.burnTime > 0 && this.pendingProduct == null) {
				// Not working if there is no water available.
				boolean hasLiquid = this.resourceTank.getFluidAmount() > 0;
				if (errorLogic.setCondition(!hasLiquid, ForestryError.NO_RESOURCE_LIQUID)) {
					return;
				}

				checkRecipe();

				if (this.currentRecipe == null) {
					return;
				}

	            FluidHelper.drain(this.resourceTank, 1);
	            this.burnTime -= speed;
	            this.productionTime -= speed;

				if (this.productionTime <= 0) {
	                this.pendingProduct = this.currentProduct;
					getTransferHandler().extract(InventoryMoistener.SLOT_RESOURCE, ItemResource.of(getItem(InventoryMoistener.SLOT_RESOURCE)), 1, transaction);
					resetRecipe();
					tryAddPending();
				}

			} else if (this.pendingProduct != null) {
				tryAddPending();
			}
			// Try to start process
			else { // Make sure we have a new item in the working slot.
				if (rotateWorkingSlot()) {
					checkRecipe();

					// Let's see if we have a valid resource in the working slot
					if (getItem(InventoryMoistener.SLOT_WORKING).isEmpty()) {
						return;
					}

					MoistenerFuel res = getItem(InventoryMoistener.SLOT_WORKING).typeHolder().getData(ForestryDataMaps.MOISTENER_FUELS);
					if (res != null) {
	                    this.burnTime = this.totalTime = res.moistenerValue();
					}
				} else {
					rotateReservoir();
				}
			}

			errorLogic.setCondition(this.currentRecipe == null, ForestryError.NO_RECIPE);
			transaction.commit();
		}
	}

	private boolean tryAddPending() {
		if (this.pendingProduct == null) {
			return false;
		}

		boolean added = InventoryUtil.tryAddStack(this, this.pendingProduct, InventoryMoistener.SLOT_PRODUCT, 1, true);
		getErrorLogic().setCondition(!added, ForestryError.NO_SPACE_INVENTORY);

		if (added) {
            this.pendingProduct = null;
		}

		return added;
	}

	public void checkRecipe() {
		RecipeMap manager = RecipeUtils.getRecipeMap();
		IMoistenerRecipe sameRec = null;
		if (manager != null) {
			sameRec = RecipeUtils.getMoistenerRecipe(manager, getInternalInventory().getItem(InventoryMoistener.SLOT_RESOURCE));
		}
		if (this.currentRecipe != sameRec) {
			this.currentRecipe = sameRec;
			String id = sameRec == null ? "" : sameRec.getId().toString();
			if (!this.activeRecipe.equals(id) || sameRec != null && this.timePerItem != sameRec.getTimePerItem()) resetRecipe();
			this.activeRecipe = id;
		}

		getErrorLogic().setCondition(this.currentRecipe == null, ForestryError.NO_RECIPE);
	}

	private void resetRecipe() {
		if (this.currentRecipe == null) {
            this.currentProduct = null;
            this.productionTime = 0;
            this.timePerItem = 0;
		} else {
            this.currentProduct = this.currentRecipe.getProduct();
            this.productionTime = this.currentRecipe.getTimePerItem();
            this.timePerItem = this.currentRecipe.getTimePerItem();
		}
	}

	private int getFreeSlot(ItemStack deposit, int startSlot, int endSlot, boolean emptyOnly) {
		int slot = -1;

		for (int i = startSlot; i < endSlot; i++) {
			ItemStack slotStack = getItem(i);
			// Empty slots are okay.
			if (slotStack.isEmpty()) {
				if (slot < 0) {
					slot = i;
				}
				continue;
			}

			if (emptyOnly) {
				continue;
			}

			// Wrong item or full
			if (!ItemStack.isSameItemSameComponents(slotStack, deposit) || slotStack.getCount() + deposit.getCount() > slotStack.getMaxStackSize()) {
				continue;
			}

			slot = i;
		}

		return slot;
	}

	private int getFreeStashSlot(ItemStack deposit, boolean emptyOnly) {
		return getFreeSlot(deposit, 0, InventoryMoistener.SLOT_RESERVOIR_1, emptyOnly);
	}

	private int getFreeReservoirSlot(ItemStack deposit) {
		return getFreeSlot(deposit, InventoryMoistener.SLOT_RESERVOIR_1, InventoryMoistener.SLOT_RESERVOIR_1 + InventoryMoistener.SLOT_RESERVOIR_COUNT, false);

	}

	private int getNextResourceSlot(int startSlot, int endSlot) {
		// Let's look for a new resource to put into the working slot.
		int stage = -1;
		int resourceSlot = -1;

		IInventoryAdapter inventory = getInternalInventory();
		for (int i = startSlot; i < endSlot; i++) {
			ItemStack slotStack = inventory.getItem(i);
			if (slotStack.isEmpty()) {
				continue;
			}

			MoistenerFuel res = slotStack.typeHolder().getData(ForestryDataMaps.MOISTENER_FUELS);
			if (res == null) {
				continue;
			}

			if (stage < 0 || res.stage() < stage) {
				stage = res.stage();
				resourceSlot = i;
			}
		}

		return resourceSlot;
	}

	private boolean rotateWorkingSlot() {
		IErrorLogic errorLogic = getErrorLogic();

		// Put working slot contents into inventory if space is available
		if (!getItem(InventoryMoistener.SLOT_WORKING).isEmpty()) {
			// Get the result of the consumed item in the working slot
			ItemStack deposit;
			MoistenerFuel res = getItem(InventoryMoistener.SLOT_WORKING).typeHolder().getData(ForestryDataMaps.MOISTENER_FUELS);
			if (res != null) {
				deposit = res.product().copy();
			} else {
				deposit = getItem(InventoryMoistener.SLOT_WORKING).copy();
			}

			int targetSlot = getFreeReservoirSlot(deposit);
			// We stop the whole thing, if we don't have any room anymore.
			if (errorLogic.setCondition(targetSlot < 0, ForestryError.NO_SPACE_INVENTORY)) {
				return false;
			}

			var transaction = Transaction.getCurrentOpenedTransaction();
			getTransferHandler().insert(targetSlot, ItemResource.of(deposit), deposit.getCount(), transaction);
			getTransferHandler().extract(InventoryMoistener.SLOT_WORKING, ItemResource.of(getItem(InventoryMoistener.SLOT_WORKING)), 1, transaction);
		}

		if (!getItem(InventoryMoistener.SLOT_WORKING).isEmpty()) {
			return true;
		}

		// Let's look for a new resource to put into the working slot.
		int resourceSlot = getNextResourceSlot(InventoryMoistener.SLOT_RESERVOIR_1, InventoryMoistener.SLOT_RESERVOIR_1 + InventoryMoistener.SLOT_RESERVOIR_COUNT);
		// Nothing found, stop.
		if (errorLogic.setCondition(resourceSlot < 0, ForestryError.NO_RESOURCE)) {
			return false;
		}

		moveItems(resourceSlot, InventoryMoistener.SLOT_WORKING, 1);
		return true;
	}

	private void rotateReservoir() {
		ArrayList<Integer> slotsToShift = new ArrayList<>();

		for (int i = InventoryMoistener.SLOT_RESERVOIR_1; i < InventoryMoistener.SLOT_RESERVOIR_1 + InventoryMoistener.SLOT_RESERVOIR_COUNT; i++) {
			if (getItem(i).isEmpty()) {
				continue;
			}

			if (getItem(i).typeHolder().getData(ForestryDataMaps.MOISTENER_FUELS) == null) {
				slotsToShift.add(i);
			}
		}

		// Move consumed items back to stash
		int shiftedSlots = 0;
		for (int slot : slotsToShift) {
			ItemStack slotStack = getItem(slot);
			int targetSlot = getFreeStashSlot(slotStack, true);
			if (targetSlot < 0) {
				continue;
			}

			moveItems(slot, targetSlot, slotStack.getCount());
			shiftedSlots++;
		}

		// Grab new items from stash
		for (int i = 0; i < (!slotsToShift.isEmpty() ? shiftedSlots : 2); i++) {
			int resourceSlot = getNextResourceSlot(0, InventoryMoistener.SLOT_RESERVOIR_1);
			// Stop if no resources are available
			if (resourceSlot < 0) {
				break;
			}
			int targetSlot = getFreeReservoirSlot(getItem(resourceSlot));
			// No free target slot, stop
			if (targetSlot < 0) {
				break;
			}
			moveItems(resourceSlot, targetSlot, getItem(resourceSlot).getCount());
		}
	}

	private void moveItems(int source, int target, int amount) {
		var inventory = getTransferHandler();
		var resource = ItemResource.of(getItem(source));
		var transaction = Transaction.getCurrentOpenedTransaction();
		int moved = inventory.insert(target, resource, amount, transaction);
		inventory.extract(source, resource, moved, transaction);
	}

	public boolean isWorking() {
		return this.burnTime > 0 && this.resourceTank.getFluidAmount() > 0;
	}

	public boolean hasFuelMin(float percentage) {
		int max = 0;
		int avail = 0;
		IInventoryAdapter inventory = getInternalInventory();

		for (int i = InventoryMoistener.SLOT_STASH_1; i < InventoryMoistener.SLOT_RESERVOIR_1; i++) {
			if (inventory.getItem(i).isEmpty()) {
				max += 64;
				continue;
			}
			if (inventory.getItem(i).typeHolder().getData(ForestryDataMaps.MOISTENER_FUELS) != null) {
				max += 64;
				avail += inventory.getItem(i).getCount();
			}
		}

		return (float) avail / (float) max > percentage;
	}

	public boolean hasResourcesMin(float percentage) {
		IInventoryAdapter inventory = getInternalInventory();
		if (inventory.getItem(InventoryMoistener.SLOT_RESOURCE).isEmpty()) {
			return false;
		}

		return (float) inventory.getItem(InventoryMoistener.SLOT_RESOURCE).getCount() / (float) inventory.getItem(InventoryMoistener.SLOT_RESOURCE).getMaxStackSize() > percentage;
	}

	public boolean isProducing() {
		return this.productionTime > 0;
	}

	public int getProductionProgressScaled(int i) {
		if (this.timePerItem == 0) {
			return 0;
		}

		return this.productionTime * i / this.timePerItem;

	}

	public int getConsumptionProgressScaled(int i) {
		if (this.totalTime == 0) {
			return 0;
		}

		return this.burnTime * i / this.totalTime;

	}

	public int getResourceScaled(int i) {
		return this.resourceTank.getFluidAmount() * i / Constants.PROCESSOR_TANK_CAPACITY;
	}

	/* IRenderableTile */
	@Override
	public TankRenderInfo getResourceTankInfo() {
		return new TankRenderInfo(this.resourceTank);
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return TankRenderInfo.EMPTY;
	}


	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	/* SMP GUI */
	public void getGUINetworkData(int i, int j) {
		switch (i) {
			case 0 -> this.burnTime = j;
			case 1 -> this.totalTime = j;
			case 2 -> this.productionTime = j;
			case 3 -> this.timePerItem = j;
		}
	}

	public void sendGUINetworkData(AbstractContainerMenu container, ContainerListener iCrafting) {
		iCrafting.dataChanged(container, 0, this.burnTime);
		iCrafting.dataChanged(container, 1, this.totalTime);
		iCrafting.dataChanged(container, 2, this.productionTime);
		iCrafting.dataChanged(container, 3, this.timePerItem);
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerMoistener(windowId, inv, this);
	}

	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}
}
