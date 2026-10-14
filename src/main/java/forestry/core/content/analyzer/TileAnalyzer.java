package forestry.core.content.analyzer;

import forestry.api.core.ForestryError;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.config.Constants;
import forestry.core.features.CoreTiles;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidTagFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.core.content.analyzer.ContainerAnalyzer;
import forestry.core.platform.inventory.InventoryAnalyzer;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.core.platform.network.packets.PacketItemStackDisplay;
import forestry.core.platform.util.GeneticsUtil;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import forestry.core.platform.tile.IItemStackDisplay;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileAnalyzer extends TilePowered implements WorldlyContainer, ILiquidTankTile, IItemStackDisplay {
	private static final int TIME_TO_ANALYZE = 125;
	private static final int HONEY_REQUIRED = 100;
	// Genetics
	public static int analyzerEnergyPerWork = 20320;

	private final FilteredTank resourceTank;
	private final TankManager tankManager;
	private final Container invInput;
	private final Container invOutput;

	@Nullable
	private IIndividual specimenToAnalyze;
	private ItemStack individualOnDisplayClient = ItemStack.EMPTY;
	@Override protected Object captureProcessingState() { return this.specimenToAnalyze; }
	@Override protected void restoreProcessingState(Object state) { this.specimenToAnalyze = (IIndividual) state; }

	/* CONSTRUCTOR */
	public TileAnalyzer(BlockPos pos, BlockState state) {
		super(CoreTiles.ANALYZER.tileType(), pos, state, 800, Constants.MACHINE_MAX_ENERGY);
		setInternalInventory(new InventoryAnalyzer(this));
        this.resourceTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY).setFilter(FluidTagFilter.HONEY);
        this.tankManager = new TankManager(this, this.resourceTank);
        this.invInput = new InventoryMapper(getInternalInventory(), InventoryAnalyzer.SLOT_INPUT_1, InventoryAnalyzer.SLOT_INPUT_COUNT);
        this.invOutput = new InventoryMapper(getInternalInventory(), InventoryAnalyzer.SLOT_OUTPUT_1, InventoryAnalyzer.SLOT_OUTPUT_COUNT);
	}

	/* SAVING & LOADING */

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.tankManager.write(output);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.tankManager.read(compoundNBT, registries);

		ItemStack stackToAnalyze = getItem(InventoryAnalyzer.SLOT_ANALYZE);
		this.specimenToAnalyze = IIndividualHandlerItem.getIndividual(stackToAnalyze);
		if (this.specimenToAnalyze != null) configureSpecimen();
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (updateOnInterval(20)) {
			// Check if we have suitable items waiting in the can slot
			FluidHelper.drainContainers(this.tankManager, this, InventoryAnalyzer.SLOT_CAN);
		}
	}

	/* WORKING */
	@Override
	public boolean workCycle() {
		try (var transaction = beginWorkTransaction()) {
			ItemStack input = getItem(InventoryAnalyzer.SLOT_ANALYZE);
			ItemStack output = input.copy();
			IIndividual specimen = IIndividualHandlerItem.getIndividual(output);
			if (specimen == null) return false;
			if (!specimen.isAnalyzed()) {
				if (FluidHelper.drain(this.resourceTank.internal(), HONEY_REQUIRED, transaction).getAmount() != HONEY_REQUIRED) return false;
				specimen.analyze();
				specimen.saveToStack(output);
			}
			if (!InventoryUtil.tryAddStack(this.invOutput, output, true)) return false;
			if (getTransferHandler().extract(InventoryAnalyzer.SLOT_ANALYZE, net.neoforged.neoforge.transfer.item.ItemResource.of(input), input.getCount(), transaction) != input.getCount()) return false;
			this.specimenToAnalyze = null;
			afterWorkCommit(() -> NetworkUtil.sendNetworkPacket(new PacketItemStackDisplay(this, getIndividualOnDisplay()), this.worldPosition, this.level));
			transaction.commit();
			return true;
		}
	}

	@Nullable
	private Integer getInputSlotIndex() {
		for (int slotIndex = 0; slotIndex < this.invInput.getContainerSize(); slotIndex++) {
			ItemStack stack = this.invInput.getItem(slotIndex);
			if (IIndividualHandlerItem.isIndividual(stack)) {
				return slotIndex;
			}
		}
		return null;
	}

	/* Network */
	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
		ItemStack displayStack = getIndividualOnDisplay();
		ItemStack.OPTIONAL_STREAM_CODEC.encode(data, displayStack);
        this.tankManager.writeData(data);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
        this.individualOnDisplayClient = ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
        this.tankManager.readData(data);
	}

	@Override
	public void handleItemStackForDisplay(ItemStack itemStack) {
		if (!ItemStack.matches(itemStack, this.individualOnDisplayClient)) {
            this.individualOnDisplayClient = itemStack;
			//TODO
			BlockPos pos = getBlockPos();
			forestry.core.platform.client.ClientsideCode.markForUpdate(pos);
			//			world.markForRerender(getPos());
		}
	}

	/* STATE INFORMATION */
	@Override
	public boolean hasWork() {
		moveSpecimenToAnalyzeSlot();

		ItemStack specimen = getItem(InventoryAnalyzer.SLOT_ANALYZE);

		boolean hasSpecimen = !specimen.isEmpty();
		boolean hasResource = true;
		boolean hasSpace = true;

		if (hasSpecimen) {
			ItemStack result = specimen.copy();
			IIndividual analyzed = IIndividualHandlerItem.getIndividual(result);
			if (analyzed != null && !analyzed.isAnalyzed()) {
				analyzed.analyze();
				analyzed.saveToStack(result);
			}
			hasSpace = InventoryUtil.tryAddStack(this.invOutput, result, true, false);

			if (this.specimenToAnalyze != null && !this.specimenToAnalyze.isAnalyzed()) {
				FluidStack drained = FluidHelper.simulateDrain(this.resourceTank, HONEY_REQUIRED);
				hasResource = !drained.isEmpty() && drained.getAmount() == HONEY_REQUIRED;
			}
		}

		getErrorLogic().setCondition(!hasSpecimen, ForestryError.NO_SPECIMEN);
		getErrorLogic().setCondition(!hasResource, ForestryError.NO_RESOURCE_LIQUID);
		getErrorLogic().setCondition(!hasSpace, ForestryError.NO_SPACE_INVENTORY);

		return hasSpecimen && hasResource && hasSpace;
	}

	private void moveSpecimenToAnalyzeSlot() {
		if (!getItem(InventoryAnalyzer.SLOT_ANALYZE).isEmpty()) {
			return;
		}

		Integer slotIndex = getInputSlotIndex();
		if (slotIndex == null) {
			return;
		}

		ItemStack original = this.invInput.getItem(slotIndex);
		ItemStack inputStack = original.copy();
		if (inputStack.isEmpty()) {
			return;
		}

		if (!SpeciesUtil.TREE_TYPE.get().isMember(inputStack)) {
			inputStack = GeneticsUtil.convertToGeneticEquivalent(inputStack);
		}

		try (var transaction = beginWorkTransaction()) {
        this.specimenToAnalyze = IIndividualHandlerItem.getIndividual(inputStack);
		if (this.specimenToAnalyze == null) {
			return;
		}

		if (getTransferHandler().extract(InventoryAnalyzer.SLOT_INPUT_1 + slotIndex, net.neoforged.neoforge.transfer.item.ItemResource.of(original), original.getCount(), transaction) != original.getCount()) return;
		if (getTransferHandler().insert(InventoryAnalyzer.SLOT_ANALYZE, net.neoforged.neoforge.transfer.item.ItemResource.of(inputStack), inputStack.getCount(), transaction) != inputStack.getCount()) return;
		configureSpecimen();
		afterWorkCommit(() -> NetworkUtil.sendNetworkPacket(new PacketItemStackDisplay(this, getIndividualOnDisplay()), this.worldPosition, this.level));
		transaction.commit();
		}
	}

	private void configureSpecimen() {
		if (this.specimenToAnalyze.isAnalyzed()) {
			configureWork("forestry:analyzed", 1, 0);
		} else {
			configureWork("forestry:analyze", TIME_TO_ANALYZE, analyzerEnergyPerWork);
		}
	}

	public ItemStack getIndividualOnDisplay() {
		// null in BEWLR
		if (this.level == null || this.level.isClientSide()) {
			return this.individualOnDisplayClient;
		}
		return getItem(InventoryAnalyzer.SLOT_ANALYZE);
	}

	/* ILiquidTankTile */

	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerAnalyzer(windowId, player.getInventory(), this);
	}
}
