package forestry.core.content.energy;

import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.core.platform.network.IStreamable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import team.reborn.energy.api.EnergyStorage;
import forestry.core.platform.transfer.CommitParticipant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** One legacy Forestry energy unit remains one Energy API unit; recipe costs are unchanged. */
public class ForestryEnergyStorage extends CommitParticipant<Long> implements EnergyStorage, IStreamable, INbtReadable, INbtWritable {
	private final long capacity;
	private final long maxReceive;
	private final long maxExtract;
	private long energy;
	private Runnable changed = () -> {};
	private final EnergyStorage chainedInput = new EnergyStorage() {
		public long getAmount() { return energy; }
		public long getCapacity() { return capacity; }
		public long insert(long amount, TransactionContext transaction) { return insertLimited(amount, Long.MAX_VALUE, transaction); }
		public long extract(long amount, TransactionContext transaction) { return ForestryEnergyStorage.this.extract(amount, transaction); }
	};

	public ForestryEnergyStorage(long maxTransfer, long capacity, EnergyTransferMode mode) {
		StoragePreconditions.notNegative(maxTransfer);
		StoragePreconditions.notNegative(capacity);
		this.capacity = capacity;
		this.maxReceive = mode.canReceive() ? maxTransfer : 0;
		this.maxExtract = mode.canExtract() ? maxTransfer : 0;
	}

	public ForestryEnergyStorage onChange(Runnable changed) {
		this.changed = changed;
		return this;
	}

	public EnergyStorage chainedInput() {
		return this.chainedInput;
	}

	@Override
	public long getAmount() { return this.energy; }

	@Override
	public long getCapacity() { return this.capacity; }

	@Override public boolean supportsInsertion() { return this.maxReceive > 0; }
	@Override public boolean supportsExtraction() { return this.maxExtract > 0; }

	public long getEnergyStored() { return this.energy; }

	public long getMaxEnergyStored() { return this.capacity; }

	public long getMaxEnergyReceived() { return this.maxReceive; }

	@Override
	public long insert(long amount, TransactionContext transaction) {
		return insertLimited(amount, this.maxReceive, transaction);
	}

	private long insertLimited(long amount, long limit, TransactionContext transaction) {
		StoragePreconditions.notNegative(amount);
		long accepted = Math.min(this.capacity - this.energy, Math.min(amount, limit));
		if (accepted > 0) {
			updateSnapshots(transaction);
			this.energy += accepted;
		}
		return accepted;
	}

	@Override
	public long extract(long amount, TransactionContext transaction) {
		return extractLimited(amount, this.maxExtract, transaction);
	}

	/** Machine work bypasses the external transfer direction, but participates in the same journal. */
	public long extractInternal(long amount, TransactionContext transaction) {
		return extractLimited(amount, Long.MAX_VALUE, transaction);
	}

	private long extractLimited(long amount, long limit, TransactionContext transaction) {
		StoragePreconditions.notNegative(amount);
		long extracted = Math.min(this.energy, Math.min(amount, limit));
		if (extracted > 0) {
			updateSnapshots(transaction);
			this.energy -= extracted;
		}
		return extracted;
	}

	@Override
	protected Long createSnapshot() { return this.energy; }

	@Override
	protected void readSnapshot(Long snapshot) { this.energy = snapshot; }

	@Override
	protected void onCommitted(Long previous) {
		if (this.energy != previous) {
			this.changed.run();
		}
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries) {
		setEnergyStored(nbt.getLongOr("Energy", 0L));
	}

	@Override
	public CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
		nbt.putLong("Energy", this.energy);
		return nbt;
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) { data.writeVarLong(this.energy); }

	@Override
	public void readData(RegistryFriendlyByteBuf data) { setEnergyStored(data.readVarLong()); }

	public void drainEnergy(long amount) {
		StoragePreconditions.notNegative(amount);
		setEnergyStored(Math.max(0, this.energy - amount));
	}

	public void generateEnergy(long amount) {
		StoragePreconditions.notNegative(amount);
		setEnergyStored(this.energy + Math.min(this.capacity - this.energy, amount));
	}

	public void setEnergyStored(long energyStored) {
		long value = Mth.clamp(energyStored, 0, this.capacity);
		if (this.energy != value) {
			try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
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
