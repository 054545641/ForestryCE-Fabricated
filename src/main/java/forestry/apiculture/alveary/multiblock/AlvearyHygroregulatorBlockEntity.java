package forestry.apiculture.alveary.multiblock;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import forestry.api.core.climate.IClimateControlled;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.api.core.machines.IHygroregulatorRecipe;
import forestry.apiculture.features.ApicultureRecipeTypes;
import forestry.apiculture.alveary.AlvearyHygroregulatorMenu;
import forestry.apiculture.alveary.AlvearyHygroregulatorInventory;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidRecipeFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.util.RecipeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class AlvearyHygroregulatorBlockEntity extends AbstractAlvearyBlockEntity implements Container, ILiquidTankTile, IAlvearyComponent.Climatiser<AlvearyMultiblockLogic>, IAlvearyComponent.HasInventory {
	// Both of these lived in core (FluidRecipeFilter and RecipeUtils) and named the hygroregulator
	// recipe type, which is apiculture's. This tile is their only consumer
	private static final FluidRecipeFilter HYGROREGULATOR_INPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, ApicultureRecipeTypes.HYGROREGULATOR.type(), IHygroregulatorRecipe::getInputFluid));

	private final TankManager tankManager;
	private final FilteredTank liquidTank;
	private final IInventoryAdapter inventory;

	@Nullable
	private IHygroregulatorRecipe currentRecipe;
	// number of ticks the current temperature change lasts for
	private int heatTicks;

	private record WorkState(int ticks, IHygroregulatorRecipe recipe) {}
	private final SnapshotJournal<WorkState> workJournal = new SnapshotJournal<>() {
		@Override protected WorkState createSnapshot() { return new WorkState(heatTicks, currentRecipe); }
		@Override protected void revertToSnapshot(WorkState previous) { heatTicks = previous.ticks(); currentRecipe = previous.recipe(); }
		@Override protected void onRootCommit(WorkState previous) { if (heatTicks != previous.ticks() || currentRecipe != previous.recipe()) setChanged(); }
	};

	public AlvearyHygroregulatorBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.HYGROREGULATOR, pos, state);

		this.inventory = new AlvearyHygroregulatorInventory(this);
		this.liquidTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY).setFilter(HYGROREGULATOR_INPUT);
		this.tankManager = new TankManager(this, this.liquidTank);
	}

	@Override
	public IInventoryAdapter getInternalInventory() {
		return this.inventory;
	}

	@Override
	public boolean allowsAutomation() {
		return true;
	}

	/* UPDATING */
	@Override
	public void changeClimate(int tickCount, IClimateControlled climateControlled) {
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			this.workJournal.updateSnapshots(transaction);
			if (this.heatTicks <= 0) {
				this.currentRecipe = getHygroRegulatorRecipe(RecipeUtils.getRecipeMap(), this.liquidTank.getFluid());
				if (this.currentRecipe != null) {
					var fluid = this.currentRecipe.getInputFluid();
					if (this.liquidTank.extractInternal(FluidResource.of(fluid), fluid.getAmount(), transaction) != fluid.getAmount()) return;
					this.heatTicks = this.currentRecipe.getRetainTime() > 0 ? this.currentRecipe.getRetainTime() : 20;
				}
			}
			if (this.heatTicks > 0) {
				this.heatTicks--;
				if (this.currentRecipe != null) {
					byte humidity = this.currentRecipe.getHumiditySteps();
					byte temperature = this.currentRecipe.getTemperatureSteps();
					new RootCommitJournal(() -> {
						climateControlled.addHumidityChange(humidity);
						climateControlled.addTemperatureChange(temperature);
					}).updateSnapshots(transaction);
				} else this.heatTicks = 0;
			}
			if (tickCount % 20 == 0) FluidHelper.drainContainers(this.tankManager, this, 0);
			transaction.commit();
		}
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.tankManager.read(compoundNBT, registries);

        this.heatTicks = compoundNBT.getIntOr("TransferTime", 0);

		FluidStack liquid = compoundNBT.getCompound("CurrentLiquid")
			.flatMap(tag -> FluidStack.OPTIONAL_CODEC.parse(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries), tag).result())
			.orElse(FluidStack.EMPTY);
		this.currentRecipe = null;
		if (!liquid.isEmpty()) {
			this.currentRecipe = getHygroRegulatorRecipe(RecipeUtils.getRecipeMap(), liquid);
		}
	}


	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.tankManager.write(output);

		compoundNBT.putInt("TransferTime", this.heatTicks);
		if (this.currentRecipe != null) {
			FluidStack.OPTIONAL_CODEC.encodeStart(net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries), this.currentRecipe.getInputFluid())
				.result().ifPresent(tag -> compoundNBT.put("CurrentLiquid", tag));
		}

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	/* ILIQUIDTANKCONTAINER */
	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	@Nullable
	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new AlvearyHygroregulatorMenu(windowId, inv, this);
	}

	@Nullable
	private static IHygroregulatorRecipe getHygroRegulatorRecipe(RecipeMap manager, FluidStack input) {
		return RecipeUtils.getMatchingRecipe(manager, ApicultureRecipeTypes.HYGROREGULATOR, recipe -> FluidStack.isSameFluidSameComponents(input, recipe.getInputFluid()) && input.getAmount() >= recipe.getInputFluid().getAmount());
	}
}
