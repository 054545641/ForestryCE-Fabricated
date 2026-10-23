package forestry.core.platform.inventory;

import forestry.core.platform.transfer.CommitParticipant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** One Fabric snapshot participant per backing inventory; every sided view shares it. */
public final class InventoryTransferHandler extends CommitParticipant<List<ItemStack>> implements SlottedStorage<ItemVariant> {
    private final forestry.api.core.ITransactionalContainer inventory;
    private final Consumer<int[]> committed;
    private final List<SingleSlotStorage<ItemVariant>> slots;
    private long version;

    public InventoryTransferHandler(InventoryPlain inventory, Consumer<int[]> committed) {
        this(inventory, inventory.getContainerSize(), committed);
    }
    public InventoryTransferHandler(forestry.api.core.ITransactionalContainer inventory, int size, Consumer<int[]> committed) {
        this.inventory = inventory;
        this.committed = committed;
        // InventoryPlain constructs the handler after its contents have been initialized.
        this.slots = java.util.stream.IntStream.range(0, size)
            .<SingleSlotStorage<ItemVariant>>mapToObj(Slot::new).toList();
    }

    @Override public int getSlotCount() { return slots.size(); }
    @Override public SingleSlotStorage<ItemVariant> getSlot(int slot) { return slots.get(slot); }
    @Override public Iterator<StorageView<ItemVariant>> iterator() { return new ArrayList<StorageView<ItemVariant>>(slots).iterator(); }
    @Override public long getVersion() { return version; }
    @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        long moved = 0;
        for (var slot : slots) moved += slot.insert(resource, amount - moved, transaction);
        return moved;
    }
    @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        long moved = 0;
        for (var slot : slots) moved += slot.extract(resource, amount - moved, transaction);
        return moved;
    }
    @Override protected List<ItemStack> createSnapshot() {
        return java.util.stream.IntStream.range(0, getSlotCount()).mapToObj(i -> inventory.getItem(i).copy()).toList();
    }
    @Override protected void readSnapshot(List<ItemStack> snapshot) {
        for (int i = 0; i < snapshot.size(); i++) inventory.setItemInTransaction(i, snapshot.get(i).copy());
    }
    @Override protected void onCommitted(List<ItemStack> original) {
        int[] changed = java.util.stream.IntStream.range(0, getSlotCount())
            .filter(i -> !ItemStack.matches(original.get(i), inventory.getItem(i))).toArray();
        if (changed.length > 0) { version++; committed.accept(changed); }
    }

    private final class Slot implements SingleSlotStorage<ItemVariant> {
        private final int index;
        private Slot(int index) { this.index = index; }
        @Override public ItemVariant getResource() { return ItemVariant.of(inventory.getItem(index)); }
        @Override public boolean isResourceBlank() { return inventory.getItem(index).isEmpty(); }
        @Override public long getAmount() { return inventory.getItem(index).getCount(); }
        @Override public long getCapacity() { return isResourceBlank() ? inventory.getMaxStackSize() : inventory.getMaxStackSize(inventory.getItem(index)); }
        @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            if (!isResourceBlank() && !getResource().equals(resource)) return 0;
            long capacity = inventory.getMaxStackSize(resource.toStack());
            int accepted = (int) Math.min(amount, Math.max(0, capacity - getAmount()));
            if (accepted > 0) {
                updateSnapshots(transaction);
                inventory.setItemInTransaction(index, resource.toStack((int) getAmount() + accepted));
            }
            return accepted;
        }
        @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, amount);
            if (!getResource().equals(resource)) return 0;
            int extracted = (int) Math.min(amount, getAmount());
            if (extracted > 0) {
                updateSnapshots(transaction);
                inventory.setItemInTransaction(index, resource.toStack((int) getAmount() - extracted));
            }
            return extracted;
        }
    }
}
