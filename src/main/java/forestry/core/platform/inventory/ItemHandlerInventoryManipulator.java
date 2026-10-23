package forestry.core.platform.inventory;

import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import java.util.function.Predicate;

/** Native Storage operations also work with providers that have no numbered slots. */
public class ItemHandlerInventoryManipulator {
    private final Storage<ItemVariant> inv;
    public ItemHandlerInventoryManipulator(Storage<ItemVariant> inv) { this.inv = inv; }
    public ItemStack tryAddStack(ItemStack stack) { return addStack(stack, false); }
    public ItemStack addStack(ItemStack stack) { return addStack(stack, true); }
    public ItemStack removeItem(Predicate<ItemStack> filter) {
        try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            for (var view : inv.nonEmptyViews()) {
                var resource = view.getResource();
                if (filter.test(resource.toStack()) && view.extract(resource, 1, tx) == 1) { tx.commit(); return resource.toStack(); }
            }
            return ItemStack.EMPTY;
        }
    }
    public boolean transferOneStack(Storage<ItemVariant> target, Predicate<ItemStack> filter) { return transferStacks(target, filter, true); }
    public boolean transferStacks(Storage<ItemVariant> target, Predicate<ItemStack> filter) { return transferStacks(target, filter, false); }
    private boolean transferStacks(Storage<ItemVariant> target, Predicate<ItemStack> filter, boolean singleStack) {
        if (inv == target) return false;
        try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            if (!singleStack) { long moved = StorageUtil.move(inv, target, resource -> filter.test(resource.toStack()), Long.MAX_VALUE, tx); tx.commit(); return moved > 0; }
            for (var view : inv.nonEmptyViews()) {
                var resource = view.getResource();
                if (!filter.test(resource.toStack())) continue;
                long moved = StorageUtil.move(inv, target, resource::equals, Math.min(view.getAmount(), resource.toStack().getMaxStackSize()), tx);
                if (moved > 0) { tx.commit(); return true; }
            }
            return false;
        }
    }
    protected ItemStack addStack(ItemStack stack, boolean execute) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
            long moved = inv.insert(ItemVariant.of(stack), stack.getCount(), tx);
            if (execute) tx.commit();
            return stack.copyWithCount(stack.getCount() - Math.toIntExact(moved));
        }
    }
}
