package forestry.core.content.sorting.inventory;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import java.util.List;
import java.util.function.Function;
/** A virtual input routes into real directional buffers inside the caller's transaction. */
public final class ItemHandlerFilter implements SingleSlotStorage<ItemVariant>, SlottedStorage<ItemVariant> {
    private final SlottedStorage<ItemVariant> inventory;
    private final Function<ItemStack, List<Direction>> directions;
    public ItemHandlerFilter(SlottedStorage<ItemVariant> inventory, Function<ItemStack, List<Direction>> directions) { this.inventory = inventory; this.directions = directions; }
    @Override public ItemVariant getResource() { return ItemVariant.blank(); }
    @Override public boolean isResourceBlank() { return true; }
    @Override public long getAmount() { return 0; }
    @Override public long getCapacity() { return 64; }
    @Override public int getSlotCount() { return 1; }
    @Override public SingleSlotStorage<ItemVariant> getSlot(int slot) { if (slot != 0) throw new IndexOutOfBoundsException(slot); return this; }
    @Override public boolean supportsExtraction() { return false; }
    @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        long inserted = 0;
        for (Direction direction : directions.apply(resource.toStack((int) Math.min(amount, Integer.MAX_VALUE)))) {
            inserted += inventory.getSlot(direction.get3DDataValue()).insert(resource, amount - inserted, transaction);
            if (inserted == amount) break;
        }
        return inserted;
    }
    @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) { StoragePreconditions.notBlankNotNegative(resource, amount); return 0; }
}
