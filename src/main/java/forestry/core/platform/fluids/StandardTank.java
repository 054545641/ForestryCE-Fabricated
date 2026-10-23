package forestry.core.platform.fluids;

import forestry.api.core.fluids.FluidUnits;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.platform.network.IStreamable;
import forestry.core.platform.transfer.CommitParticipant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.Objects;
import java.util.function.Predicate;

/** Exact droplet storage. Legacy constructor capacities are explicitly in mB. */
public class StandardTank extends CommitParticipant<StandardTank.Contents> implements SingleSlotStorage<FluidVariant>, IStreamable {
    private final long initialCapacity;
    private long capacity;
    private final boolean canFill;
    private final boolean canDrain;
    private boolean variableCapacity;
    private FluidVolume fluid = FluidVolume.EMPTY;
    private Predicate<FluidVolume> validator = value -> true;
    private ITankUpdateHandler updateHandler = tank -> {};
    private int tankIndex;
    private long version;
    private final SingleSlotStorage<FluidVariant> internal = new InternalStorage();

    public StandardTank(int capacityMilliBuckets, boolean canFill, boolean canDrain) {
        this(FluidUnits.fromMilliBuckets(capacityMilliBuckets), canFill, canDrain);
    }
    private StandardTank(long capacityDroplets, boolean canFill, boolean canDrain) {
        StoragePreconditions.notNegative(capacityDroplets);
        this.initialCapacity = this.capacity = capacityDroplets;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }
    public StandardTank(int capacityMilliBuckets) { this(capacityMilliBuckets, true, true); }
    public static StandardTank ofDroplets(long capacity) { return new StandardTank(capacity, true, true); }
    public static StandardTank variableCapacity(int capacityMilliBuckets) {
        var tank = new StandardTank(capacityMilliBuckets, false, false);
        tank.variableCapacity = true;
        return tank;
    }
    public boolean hasVariableCapacity() { return variableCapacity; }
    public void setTankIndex(int index) { tankIndex = index; }
    public int getTankIndex() { return tankIndex; }
    public void setTankUpdateHandler(ITankUpdateHandler handler) { updateHandler = Objects.requireNonNull(handler); }
    public StandardTank setValidator(Predicate<FluidVolume> validator) { this.validator = Objects.requireNonNull(validator); return this; }
    public boolean isFluidValid(FluidVolume value) { return !value.isEmpty() && validator.test(value); }
    @Override public long getCapacity() { return capacity; }
    @Override public long getAmount() { return fluid.amount(); }
    @Override public FluidVariant getResource() { return fluid.variant(); }
    @Override public boolean isResourceBlank() { return fluid.isEmpty(); }
    @Override public long getVersion() { return version; }
    @Override public boolean supportsInsertion() { return canFill; }
    @Override public boolean supportsExtraction() { return canDrain; }
    public FluidVolume getFluid() { return fluid; }
    public net.minecraft.world.level.material.Fluid getFluidType() { return fluid.getFluid(); }
    public long getFluidAmount() { return getAmount(); }
    public boolean isEmpty() { return fluid.isEmpty(); }
    public boolean isFull() { return getAmount() >= capacity; }
    public long getRemainingSpace() { return capacity - getAmount(); }
    public SingleSlotStorage<FluidVariant> internal() { return internal; }

    @Override public long insert(FluidVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        return canFill && validator.test(new FluidVolume(resource, amount)) ? insertInternal(resource, amount, transaction) : 0;
    }
    public long insertInternal(FluidVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        if (!fluid.isEmpty() && !fluid.variant().equals(resource)) return 0;
        long accepted = Math.min(amount, getRemainingSpace());
        if (accepted > 0) {
            updateSnapshots(transaction);
            fluid = new FluidVolume(resource, getAmount() + accepted);
        }
        return accepted;
    }
    @Override public long extract(FluidVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        return canDrain ? extractInternal(resource, amount, transaction) : 0;
    }
    public long extractInternal(FluidVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        if (!resource.equals(fluid.variant())) return 0;
        long extracted = Math.min(amount, getAmount());
        if (extracted > 0) {
            updateSnapshots(transaction);
            fluid = fluid.withAmount(getAmount() - extracted);
        }
        return extracted;
    }
    public void setFluid(FluidVolume value) {
        Contents contents = validateContents(value, variableCapacity ? Math.max(capacity, value.amount()) : capacity);
        try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            updateSnapshots(transaction);
            restoreContents(contents);
            transaction.commit();
        }
    }
    public void setCapacity(long droplets, TransactionContext transaction) {
        validateContents(fluid, droplets);
        if (capacity != droplets) { updateSnapshots(transaction); capacity = droplets; }
    }
    public void setCapacity(long droplets) {
        try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            setCapacity(droplets, transaction); transaction.commit();
        }
    }
    public record Contents(FluidVolume fluid, long capacity) {}
    @Override protected Contents createSnapshot() { return new Contents(fluid, capacity); }
    @Override protected void readSnapshot(Contents value) { restoreContents(value); }
    @Override protected void onCommitted(Contents original) {
        if (!original.equals(createSnapshot())) { version++; updateHandler.updateTankLevels(this); }
    }
    Contents validateContents(FluidVolume value, long capacity) {
        if (capacity < 0 || (!variableCapacity && capacity != initialCapacity) || value.amount() > capacity) {
            throw new IllegalArgumentException("Invalid tank contents: " + value.amount() + "/" + capacity);
        }
        return new Contents(value, capacity);
    }
    Contents emptyContents() { return new Contents(FluidVolume.EMPTY, initialCapacity); }
    void restoreContents(Contents contents) { fluid = contents.fluid; capacity = contents.capacity; }
    Contents decodeContents(HolderLookup.Provider registries, CompoundTag tag) {
        var value = FluidVolume.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
        long restoredCapacity = initialCapacity;
        if (variableCapacity) {
            if (tag.contains("capacity")) {
                long savedCapacity = tag.getLong("capacity").orElseThrow(() -> new IllegalArgumentException("Invalid tank capacity"));
                restoredCapacity = tag.getIntOr("schema", 1) == 1 ? FluidUnits.fromMilliBuckets(savedCapacity) : savedCapacity;
            } else restoredCapacity = Math.max(initialCapacity, value.amount());
        }
        return validateContents(value, restoredCapacity);
    }
    public CompoundTag writeToNBT(HolderLookup.Provider registries, CompoundTag tag) {
        var encoded = (CompoundTag) FluidVolume.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow();
        for (String key : new String[]{"schema", "unit", "id", "amount", "components", "capacity"}) tag.remove(key);
        tag.merge(encoded);
        if (variableCapacity) tag.putLong("capacity", capacity);
        return tag;
    }
    public void readFromNBT(HolderLookup.Provider registries, CompoundTag tag) { restoreContents(decodeContents(registries, tag)); }
    @Override public void writeData(RegistryFriendlyByteBuf data) {
        FluidVolume.STREAM_CODEC.encode(data, fluid);
        if (variableCapacity) data.writeVarLong(capacity);
    }
    Contents decodeData(RegistryFriendlyByteBuf data) {
        var value = FluidVolume.STREAM_CODEC.decode(data);
        return validateContents(value, variableCapacity ? data.readVarLong() : initialCapacity);
    }
    @Override public void readData(RegistryFriendlyByteBuf data) { restoreContents(decodeData(data)); }

    private final class InternalStorage implements SingleSlotStorage<FluidVariant> {
        @Override public long insert(FluidVariant resource, long amount, TransactionContext transaction) { return insertInternal(resource, amount, transaction); }
        @Override public long extract(FluidVariant resource, long amount, TransactionContext transaction) { return extractInternal(resource, amount, transaction); }
        @Override public FluidVariant getResource() { return StandardTank.this.getResource(); }
        @Override public boolean isResourceBlank() { return StandardTank.this.isResourceBlank(); }
        @Override public long getAmount() { return StandardTank.this.getAmount(); }
        @Override public long getCapacity() { return capacity; }
        @Override public long getVersion() { return version; }
    }
}
