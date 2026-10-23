package forestry.agriculture.multifarm.tiles;

import forestry.agriculture.multifarm.multiblock.FarmPattern;
import forestry.api.core.multiblock.IFarmComponent;
import forestry.api.core.multiblock.IFarmController;
import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.ForestryEnergyStorage;
import forestry.agriculture.features.MultifarmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import forestry.core.platform.transfer.CommitParticipant;
import forestry.core.platform.transfer.CommitAction;

public class TileFarmGearbox extends TileFarm implements IFarmComponent.Active {
	private static final int WORK_CYCLES = 4;
	private static final int ENERGY_PER_OPERATION = WORK_CYCLES * 50;

	private final ForestryEnergyStorage energyStorage;

	private int activationDelay = 0;
	private int previousDelays = 0;
	private int workCounter;

	private boolean workScheduled;
	private record WorkState(int counter, int delay, int previousDelays, boolean scheduled) {}
	private final CommitParticipant<WorkState> workJournal = new CommitParticipant<>() {
		@Override protected WorkState createSnapshot() { return new WorkState(workCounter, activationDelay, previousDelays, workScheduled); }
		@Override protected void readSnapshot(WorkState state) {
			workCounter = state.counter(); activationDelay = state.delay(); previousDelays = state.previousDelays(); workScheduled = state.scheduled();
		}
		@Override protected void onCommitted(WorkState state) { setChanged(); }
	};

	public TileFarmGearbox(BlockPos pos, BlockState state) {
		super(MultifarmBlockEntities.GEARBOX.tileType(), pos, state);

		this.energyStorage = EnergyHelper.createStorage(200, 10000, forestry.core.content.energy.EnergyTransferMode.RECEIVE, this::setChanged);
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.energyStorage.read(compoundNBT, registries);

        this.activationDelay = compoundNBT.getIntOr("ActivationDelay", 0);
        this.previousDelays = compoundNBT.getIntOr("PrevDelays", 0);
		this.workCounter = Math.clamp(compoundNBT.getIntOr("WorkCounter", 0), 0, WORK_CYCLES);
	}


	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

        this.energyStorage.write(compoundNBT, registries);

		compoundNBT.putInt("ActivationDelay", this.activationDelay);
		compoundNBT.putInt("PrevDelays", this.previousDelays);
		compoundNBT.putInt("WorkCounter", this.workCounter);
		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	public void updateServer(int tickCount) {
		if (this.workScheduled || this.workCounter < WORK_CYCLES && this.energyStorage.getEnergyStored() <= 0) return;
		try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			this.workJournal.updateSnapshots(transaction);
			if (this.activationDelay > 0) this.activationDelay--;
			else {
				if (this.workCounter < WORK_CYCLES && EnergyHelper.consumeEnergyToDoWork(this.energyStorage, WORK_CYCLES, ENERGY_PER_OPERATION)) this.workCounter++;
				if (this.workCounter >= WORK_CYCLES && tickCount % 5 == 0) {
					this.workScheduled = true;
					new CommitAction(() -> forestry.core.platform.transfer.WorldWorkQueue.enqueue(this.level, () -> {
						this.workScheduled = false;
						if (isRemoved()) return;
						if (getMultiblockLogic().getController().doWork()) {
							this.workCounter = 0; this.previousDelays = 0;
						} else {
							this.activationDelay = Math.min(10 * this.previousDelays, 120);
							this.previousDelays++;
						}
						setChanged();
					})).updateSnapshots(transaction);
				}
			}
			transaction.commit();
		}
	}

	@Override
	public void updateClient(int tickCount) {
		// todo add sided multiblock component ticking and remove this
	}

	public ForestryEnergyStorage getEnergyManager() {
		return this.energyStorage;
	}

	public EnergyStorage getEnergyStorage(Direction facing) {
		if (!this.remove) {
			return this.energyStorage;
		}
		return null;
	}

	@Override
	public String patternTypeId() {
		return FarmPattern.GEARBOX;
	}

}
