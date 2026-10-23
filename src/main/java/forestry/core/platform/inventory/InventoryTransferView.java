package forestry.core.platform.inventory;

import forestry.api.core.ITransactionalContainer;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;

/** Policy-only view; snapshots are always owned by the backing storage. */
public final class InventoryTransferView implements SlottedStorage<ItemVariant> {
    private final SlottedStorage<ItemVariant> owner;
    private final List<SingleSlotStorage<ItemVariant>> slots;

    public InventoryTransferView(SlottedStorage<ItemVariant> owner, int size, IntUnaryOperator mapping,
            BiPredicate<Integer, ItemVariant> insert, BiPredicate<Integer, ItemVariant> extract) {
        this.owner = Objects.requireNonNull(owner);
        var result = new ArrayList<SingleSlotStorage<ItemVariant>>(size);
        for (int i = 0; i < size; i++) {
            int index = mapping.applyAsInt(i);
            var backing = owner.getSlot(index);
            result.add(new SingleSlotStorage<>() {
                @Override public ItemVariant getResource() { return backing.getResource(); }
                @Override public boolean isResourceBlank() { return backing.isResourceBlank(); }
                @Override public long getAmount() { return backing.getAmount(); }
                @Override public long getCapacity() { return backing.getCapacity(); }
                @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
                    StoragePreconditions.notBlankNotNegative(resource, amount);
                    return insert.test(index, resource) ? backing.insert(resource, amount, transaction) : 0;
                }
                @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
                    StoragePreconditions.notBlankNotNegative(resource, amount);
                    return extract.test(index, resource) ? backing.extract(resource, amount, transaction) : 0;
                }
            });
        }
        this.slots = List.copyOf(result);
    }
    public static SlottedStorage<ItemVariant> internal(Container inventory) {
        return inventory instanceof ITransactionalContainer transactional ? transactional.getTransferHandler() : ContainerStorage.of(inventory, null);
    }
    public static SlottedStorage<ItemVariant> automation(Container inventory, @Nullable Direction side) {
        return new InventoryTransferView(internal(inventory), inventory.getContainerSize(), i -> i,
            (i, resource) -> allowed(inventory, side, i, resource, true),
            (i, resource) -> allowed(inventory, side, i, resource, false));
    }
    private static boolean allowed(Container inventory, @Nullable Direction side, int slot, ItemVariant resource, boolean inserting) {
        if (!(inventory instanceof WorldlyContainer worldly)) return !inserting || inventory.canPlaceItem(slot, resource.toStack());
        for (Direction face : side == null ? Direction.values() : new Direction[]{side}) {
            for (int exposed : worldly.getSlotsForFace(face)) {
                if (exposed == slot && (inserting
                    ? inventory.canPlaceItem(slot, resource.toStack()) && worldly.canPlaceItemThroughFace(slot, resource.toStack(), face)
                    : worldly.canTakeItemThroughFace(slot, resource.toStack(), face))) return true;
            }
        }
        return false;
    }
    @Override public int getSlotCount() { return slots.size(); }
    @Override public SingleSlotStorage<ItemVariant> getSlot(int index) { return slots.get(index); }
    @Override public Iterator<StorageView<ItemVariant>> iterator() { return new ArrayList<StorageView<ItemVariant>>(slots).iterator(); }
    @Override public long getVersion() { return owner.getVersion(); }
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
}
