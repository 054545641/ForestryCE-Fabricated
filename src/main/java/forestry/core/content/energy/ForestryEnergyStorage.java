package forestry.core.content.energy;

import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.core.platform.network.IStreamable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ForestryEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler, IStreamable, INbtReadable, INbtWritable {
	private final int capacity;
	private final int maxReceive;
	private final int maxExtract;
	private int energy;
	private Runnable changed = () -> {};
	private final EnergyHandler chainedInput = new EnergyHandler() {
		public long getAmountAsLong() { return energy; }
		public long getCapacityAsLong() { return capacity; }
		public int insert(int amount, TransactionContext transaction) { return insertLimited(amount, Integer.MAX_VALUE, transaction); }
		public int extract(int amount, TransactionContext transaction) { return ForestryEnergyStorage.this.extract(amount, transaction); }
	};

	public ForestryEnergyStorage(int maxTransfer, int capacity, EnergyTransferMode mode) {
		TransferPreconditions.checkNonNegative(maxTransfer);
		TransferPreconditions.checkNonNegative(capacity);
		this.capacity = capacity;
		this.maxReceive = mode.canReceive() ? maxTransfer : 0;
		this.maxExtract = mode.canExtract() ? maxTransfer : 0;
	}

	public ForestryEnergyStorage onChange(Runnable changed) {
		this.changed = changed;
		return this;
	}

	public EnergyHandler chainedInput() {
		return this.chainedInput;
	}

	@Override
	public long getAmountAsLong() { return this.energy; }

	@Override
	public long getCapacityAsLong() { return this.capacity; }

	public int getEnergyStored() { return this.energy; }

	public int getMaxEnergyStored() { return this.capacity; }

	public int getMaxEnergyReceived() { return this.maxReceive; }

	@Override
	public int insert(int amount, TransactionContext transaction) {
		return insertLimited(amount, this.maxReceive, transaction);
	}

	private int insertLimited(int amount, int limit, TransactionContext transaction) {
		TransferPreconditions.checkNonNegative(amount);
		int accepted = Math.min(this.capacity - this.energy, Math.min(amount, limit));
		if (accepted > 0) {
			updateSnapshots(transaction);
			this.energy += accepted;
		}
		return accepted;
	}

	@Override
	public int extract(int amount, TransactionContext transaction) {
		return extractLimited(amount, this.maxExtract, transaction);
	}

	/** Machine work bypasses the external transfer direction, but participates in the same journal. */
	public int extractInternal(int amount, TransactionContext transaction) {
		return extractLimited(amount, Integer.MAX_VALUE, transaction);
	}

	private int extractLimited(int amount, int limit, TransactionContext transaction) {
		TransferPreconditions.checkNonNegative(amount);
		int extracted = Math.min(this.energy, Math.min(amount, limit));
		if (extracted > 0) {
			updateSnapshots(transaction);
			this.energy -= extracted;
		}
		return extracted;
	}

	@Override
	protected Integer createSnapshot() { return this.energy; }

	@Override
	protected void revertToSnapshot(Integer snapshot) { this.energy = snapshot; }

	@Override
	protected void onRootCommit(Integer previous) {
		if (this.energy != previous) {
			this.changed.run();
		}
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries) {
		setEnergyStored(nbt.getIntOr("Energy", 0));
	}

	@Override
	public CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
		nbt.putInt("Energy", this.energy);
		return nbt;
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) { data.writeVarInt(this.energy); }

	@Override
	public void readData(RegistryFriendlyByteBuf data) { setEnergyStored(data.readVarInt()); }

	public void drainEnergy(int amount) {
		TransferPreconditions.checkNonNegative(amount);
		setEnergyStored(Math.max(0, this.energy - amount));
	}

	public void generateEnergy(int amount) {
		TransferPreconditions.checkNonNegative(amount);
		setEnergyStored((int) Math.min(this.capacity, (long) this.energy + amount));
	}

	public void setEnergyStored(int energyStored) {
		int value = Mth.clamp(energyStored, 0, this.capacity);
		if (this.energy != value) {
			try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				updateSnapshots(transaction);
				this.energy = value;
				transaction.commit();
			}
		}
	}

	public int calculateRedstone() {
		return this.capacity == 0 ? 0 : Mth.floor(((float) this.energy / this.capacity) * 14.0F) + (this.energy > 0 ? 1 : 0);
	}
}
