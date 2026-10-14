package forestry.core.platform.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.Iterator;
import java.util.function.Predicate;

public class ItemHandlerInventoryManipulator implements Iterable<IInvSlot> {
	private final ResourceHandler<ItemResource> inv;
	public ItemHandlerInventoryManipulator(ResourceHandler<ItemResource> inv) { this.inv = inv; }
	@Override public Iterator<IInvSlot> iterator() { return new InvIterator(this.inv); }
	public ItemStack tryAddStack(ItemStack stack) { return addStack(stack, false); }
	public ItemStack addStack(ItemStack stack) { return addStack(stack, true); }
	public ItemStack removeItem(Predicate<ItemStack> filter) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			var removed = ResourceHandlerUtil.extractFirst(this.inv, r -> filter.test(r.toStack()), 1, tx);
			tx.commit();
			return removed == null ? ItemStack.EMPTY : removed.resource().toStack(removed.amount());
		}
	}
	public boolean transferOneStack(ResourceHandler<ItemResource> dest, Predicate<ItemStack> filter) { return transferStacks(dest, filter, true); }
	public boolean transferStacks(ResourceHandler<ItemResource> dest, Predicate<ItemStack> filter) { return transferStacks(dest, filter, false); }
	private boolean transferStacks(ResourceHandler<ItemResource> dest, Predicate<ItemStack> filter, boolean singleStack) {
		if (this.inv == dest) return false;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			boolean moved = false;
			for (int slot = 0; slot < this.inv.size(); slot++) {
				var resource = this.inv.getResource(slot);
				if (resource.isEmpty() || !filter.test(resource.toStack(this.inv.getAmountAsInt(slot)))) continue;
				int index = slot;
				var source = new InventoryTransferView(this.inv, 1, i -> index, (i, r) -> false, (i, r) -> true);
				if (ResourceHandlerUtil.moveStacking(source, dest, r -> true, this.inv.getAmountAsInt(slot), tx) > 0) {
					moved = true;
					if (singleStack) break;
				}
			}
			tx.commit();
			return moved;
		}
	}
	protected ItemStack addStack(ItemStack stack, boolean doAdd) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			int added = ResourceHandlerUtil.insertStacking(this.inv, ItemResource.of(stack), stack.getCount(), tx);
			if (doAdd) tx.commit();
			return stack.copyWithCount(stack.getCount() - added);
		}
	}
}
