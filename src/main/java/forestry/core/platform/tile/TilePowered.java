package forestry.core.platform.tile;

import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.core.engine.circuits.IMachineUpgradable;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

/**
 * Base class for machines that consume energy to do work.
 *
 * <p>Work is measured in "steps", not game ticks. One step happens every {@link #TICKS_PER_STEP} game
 * ticks, and a work cycle takes some number of steps to complete.
 */
public abstract class TilePowered extends TileBase implements IRenderableTile, IMachineUpgradable, IStreamableGui, IPowerHandler {
	// the number of game ticks between two work steps
	private static final int TICKS_PER_STEP = 5;

	private final ForestryEnergyStorage energyStorage;
	protected float speedMultiplier = 1.0f;
	protected float powerMultiplier = 1.0f;
	protected double outputMultiplier = 1.0f;
	// The number of steps into the current work cycle. Between 0 and stepsPerWorkCycle
	private int workCounter;
	// The number of steps a work cycle takes to complete
	private int stepsPerWorkCycle;
	// The amount of energy consumed over the course of an entire work cycle
	private int energyPerWorkCycle;
	// the number of steps that this tile has had no power
	private int noPowerTime = 0;
	private String workRecipe = "";
	private boolean worldWorkScheduled;
	private long processingSeed = net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed();
	private record WorkState(int counter, int steps, int energy, String recipe, long seed, boolean scheduled, Object processing) {}
	private final SnapshotJournal<WorkState> workJournal = new SnapshotJournal<>() {
		@Override protected WorkState createSnapshot() {
			return new WorkState(workCounter, stepsPerWorkCycle, energyPerWorkCycle, workRecipe, processingSeed, worldWorkScheduled, captureProcessingState());
		}
		@Override protected void revertToSnapshot(WorkState state) {
			workCounter = state.counter();
			stepsPerWorkCycle = state.steps();
			energyPerWorkCycle = state.energy();
			workRecipe = state.recipe();
			processingSeed = state.seed();
			worldWorkScheduled = state.scheduled();
			restoreProcessingState(state.processing());
		}
		@Override protected void onRootCommit(WorkState previous) { setChanged(); }
	};

	protected Object captureProcessingState() { return null; }
	protected void restoreProcessingState(Object state) {}

	protected final Transaction beginWorkTransaction() {
		var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction());
		this.workJournal.updateSnapshots(transaction);
		return transaction;
	}

	protected final void afterWorkCommit(Runnable action) {
		var transaction = Transaction.getCurrentOpenedTransaction();
		if (transaction == null) action.run();
		else new RootCommitJournal(action).updateSnapshots(transaction);
	}

	protected final net.minecraft.util.RandomSource workRandom() {
		var random = net.minecraft.util.RandomSource.create(this.processingSeed);
		this.processingSeed = random.nextLong();
		return random;
	}

	protected final net.minecraft.util.RandomSource previewWorkRandom() {
		var random = net.minecraft.util.RandomSource.create(this.processingSeed);
		random.nextLong();
		return random;
	}

	/** Stable recipe identity preserves paid progress on reload, but resets it when the recipe changes. */
	protected final void configureWork(String recipe, int steps, int energy) {
		if (!this.workRecipe.equals(recipe) || this.stepsPerWorkCycle != Math.max(1, steps)
			|| this.energyPerWorkCycle != EnergyHelper.scaleForDifficulty(energy)) this.workCounter = 0;
		this.workRecipe = recipe;
		this.stepsPerWorkCycle = Math.max(1, steps);
		setEnergyPerWorkCycle(energy);
	}

	protected enum CycleResult { BLOCKED, PROGRESS, COMPLETE }
	protected CycleResult completeWorkCycle() { return workCycle() ? CycleResult.COMPLETE : CycleResult.BLOCKED; }

	/** World edits are irreversible side effects: spend the saved work credit only after root commit. */
	protected final CycleResult deferWorldWork(java.util.function.BooleanSupplier work) {
		this.worldWorkScheduled = true;
		afterWorkCommit(() -> {
			this.worldWorkScheduled = false;
			if (isRemoved()) return;
			if (work.getAsBoolean()) this.workCounter = 0;
			setChanged();
		});
		return CycleResult.PROGRESS;
	}

	protected TilePowered(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxTransfer, int capacity) {
		super(type, pos, state);

		this.energyStorage = EnergyHelper.createStorage(maxTransfer, capacity, EnergyTransferMode.RECEIVE, this::setChanged);

		this.stepsPerWorkCycle = 4;
	}

	public ForestryEnergyStorage getEnergyManager() {
		return this.energyStorage;
	}

	public int getWorkCounter() {
		return this.workCounter;
	}

	public void setStepsPerWorkCycle(int stepsPerWorkCycle) {
		this.stepsPerWorkCycle = stepsPerWorkCycle;
		this.workCounter = 0;
	}

	public int getStepsPerWorkCycle() {
		if (this.level.isClientSide()) {
			return this.stepsPerWorkCycle;
		}
		return Math.round(this.stepsPerWorkCycle / this.speedMultiplier);
	}

	// energy drawn per step is energyPerWorkCycle / stepsPerWorkCycle
	public void setEnergyPerWorkCycle(int energyPerWorkCycle) {
		this.energyPerWorkCycle = EnergyHelper.scaleForDifficulty(energyPerWorkCycle);
	}

	public int getEnergyPerWorkCycle() {
		return Math.round(this.energyPerWorkCycle * this.powerMultiplier);
	}

	/* STATE INFORMATION */
	public boolean hasResourcesMin(float percentage) {
		return false;
	}

	public boolean hasFuelMin(float percentage) {
		return false;
	}

	// Called every tick to determine whether the tile can start working or continue working
	public abstract boolean hasWork();

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (!updateOnInterval(TICKS_PER_STEP)) {
			return;
		}

		IErrorLogic errorLogic = getErrorLogic();

		boolean disabled = isRedstoneActivated();
		errorLogic.setCondition(disabled, ForestryError.DISABLED_BY_REDSTONE);
		if (disabled) {
			return;
		}
		processWorkStep();
	}

	/** One paid step, including final conversion, is atomic with a caller's transaction. */
	public final boolean processWorkStep() {
		if (this.worldWorkScheduled) return false;
		try (var transaction = beginWorkTransaction()) {
			if (!hasWork()) return false;
			int steps = Math.max(1, getStepsPerWorkCycle());
			if (this.workCounter < steps) {
				if (!EnergyHelper.consumeEnergyToDoWork(this.energyStorage, steps, getEnergyPerWorkCycle())) {
					if (++this.noPowerTime > 4) getErrorLogic().setCondition(true, ForestryError.NO_POWER);
					return false;
				}
				this.workCounter++;
			}
			if (this.workCounter >= steps) {
				CycleResult result = completeWorkCycle();
				if (result == CycleResult.BLOCKED) return false;
				if (result == CycleResult.COMPLETE) this.workCounter = 0;
			}
			getErrorLogic().setCondition(false, ForestryError.NO_POWER);
			this.noPowerTime = 0;
			transaction.commit();
			return true;
		}
	}

	// Called when the tile reaches the end of a work cycle. Consume inputs and produce outputs here.
	protected abstract boolean workCycle();

	// Returns the width for a progress bar. pixels is the full width of the progress bar.
	public int getProgressScaled(int pixels) {
		int stepsPerWorkCycle = getStepsPerWorkCycle();
		if (stepsPerWorkCycle == 0) {
			return 0;
		}

		return this.workCounter * pixels / stepsPerWorkCycle;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
		this.energyStorage.write(nbt, registries);
		output.putInt("WorkCounter", this.workCounter);
		output.putInt("WorkSteps", this.stepsPerWorkCycle);
		output.putInt("WorkEnergy", this.energyPerWorkCycle);
		output.putString("WorkRecipe", this.workRecipe);
		output.putLong("ProcessingSeed", this.processingSeed);

		ValueIOUtil.mergeInto(output, nbt);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
		this.speedMultiplier = 1.0f;
		this.powerMultiplier = 1.0f;
		this.outputMultiplier = 1.0;
		this.energyStorage.read(nbt, registries);
		this.workCounter = Math.max(0, input.getIntOr("WorkCounter", 0));
		this.stepsPerWorkCycle = Math.max(1, input.getIntOr("WorkSteps", this.stepsPerWorkCycle));
		this.energyPerWorkCycle = Math.max(0, input.getIntOr("WorkEnergy", this.energyPerWorkCycle));
		this.workRecipe = input.getStringOr("WorkRecipe", "");
		this.processingSeed = input.getLongOr("ProcessingSeed", this.processingSeed);
	}

	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		this.energyStorage.writeData(data);
		data.writeVarInt(this.workCounter);
		data.writeVarInt(getStepsPerWorkCycle());
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
		this.energyStorage.readData(data);
		this.workCounter = data.readVarInt();
		this.stepsPerWorkCycle = data.readVarInt();
	}

	/* IMachineUpgradable */
	protected final void loadMachineUpgrade(forestry.api.core.circuits.ICircuitBoard board) {
		int savedWork = this.workCounter;
		board.onLoad(this);
		this.workCounter = savedWork;
	}

	/* IMachineUpgradable */
	public void applyMachineUpgrade(double speedChange, double powerChange, double outputChange) {
		this.speedMultiplier += speedChange;
		this.powerMultiplier += powerChange;
		this.outputMultiplier *= outputChange;
		this.workCounter = 0;
	}

	/* IMachineUpgradable */
	@Override
	public void removeMachineUpgrade(double speedChange, double powerChange, double outputChange) {
		this.speedMultiplier -= speedChange;
		this.powerMultiplier -= powerChange;
		this.outputMultiplier /= outputChange;
		this.workCounter = 0;

		if (Float.isNaN((float) this.outputMultiplier)) {
			this.outputMultiplier = 1.0f;
		}
	}

	/* IRenderableTile */
	@Override
	public TankRenderInfo getResourceTankInfo() {
		return TankRenderInfo.EMPTY;
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return TankRenderInfo.EMPTY;
	}

	@Nullable
	public EnergyHandler getEnergyHandler(@Nullable Direction facing) {
		return this.remove ? null : this.energyStorage;
	}
}
