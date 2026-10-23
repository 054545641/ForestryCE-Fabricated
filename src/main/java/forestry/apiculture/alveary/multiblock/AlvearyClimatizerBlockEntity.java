package forestry.apiculture.alveary.multiblock;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import forestry.core.platform.transfer.CommitParticipant;
import forestry.core.platform.transfer.CommitAction;
import forestry.api.core.climate.IClimateControlled;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.core.platform.tile.IActivatable;
import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import team.reborn.energy.api.EnergyStorage;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

// Used by Heater and Fan, which increase and decrease temperature respectively
public abstract class AlvearyClimatizerBlockEntity extends AbstractAlvearyBlockEntity implements IActivatable, IAlvearyComponent.Climatiser<AlvearyMultiblockLogic> {
	private static final int TICKS_PER_CYCLE = 1;
	private static final int FE_PER_OPERATION = 50;

	private final ForestryEnergyStorage energyStorage;
	private final byte temperatureSteps;

	private int workingTime = 0;

	private final CommitParticipant<Integer> workJournal = new CommitParticipant<>() {
		@Override protected Integer createSnapshot() { return workingTime; }
		@Override protected void readSnapshot(Integer previous) { workingTime = previous; }
		@Override protected void onCommitted(Integer previous) { if (workingTime != previous) setChanged(); }
	};

	protected AlvearyClimatizerBlockEntity(AlvearyBlock.Type alvearyType, BlockPos pos, BlockState state, byte temperatureSteps) {
		super(alvearyType, pos, state);
		this.temperatureSteps = temperatureSteps;

		this.energyStorage = EnergyHelper.createStorage(1000, 2000, EnergyTransferMode.RECEIVE, this::setChanged);
	}

	/* UPDATING */
	@Override
	public void changeClimate(int tick, IClimateControlled climateControlled) {
		try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			this.workJournal.updateSnapshots(transaction);
			if (this.workingTime < 20 && EnergyHelper.consumeEnergyToDoWork(this.energyStorage, TICKS_PER_CYCLE, FE_PER_OPERATION)) {
				this.workingTime += FE_PER_OPERATION / 10;
			}
			if (this.workingTime > 0) {
				this.workingTime--;
				new CommitAction(() -> climateControlled.addTemperatureChange(this.temperatureSteps)).updateSnapshots(transaction);
			}
			new CommitAction(() -> { if (!isRemoved()) setActive(this.workingTime > 0); }).updateSnapshots(transaction);
			transaction.commit();
		}
	}

	/* LOADING & SAVING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.energyStorage.read(compoundNBT, registries);
        this.workingTime = compoundNBT.getIntOr("Heating", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.energyStorage.write(compoundNBT, registries);
		compoundNBT.putInt("Heating", this.workingTime);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	/* Network */
	@Override
	protected void encodeDescriptionPacket(CompoundTag packetData) {
		super.encodeDescriptionPacket(packetData);
	}

	@Override
	protected void decodeDescriptionPacket(CompoundTag packetData) {
		super.decodeDescriptionPacket(packetData);
	}

	/* IActivatable */
	@Override
	public boolean isActive() {
		return getBlockState().getValue(AlvearyBlock.STATE) == AlvearyBlock.State.ON;
	}

	@Override
	public void setActive(boolean active) {
		if (isActive() != active) {
			this.level.setBlockAndUpdate(this.worldPosition, getBlockState().setValue(AlvearyBlock.STATE, active ? AlvearyBlock.State.ON : AlvearyBlock.State.OFF));
		}
	}

	@Nullable
	public EnergyStorage getEnergyStorage(@Nullable Direction facing) {
		return this.remove ? null : this.energyStorage;
	}
}
