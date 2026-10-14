package forestry.core.platform.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class InvSlot implements IInvSlot {
	private final ResourceHandler<ItemResource> inv;
	protected final int slot;
	public InvSlot(ResourceHandler<ItemResource> inv, int slot) { this.inv = inv; this.slot = slot; }
	@Override public int getIndex() { return this.slot; }
	@Override public boolean canPutStackInSlot(ItemStack stack) {
		if (stack.isEmpty()) return false;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			return this.inv.insert(this.slot, ItemResource.of(stack), stack.getCount(), tx) > 0;
		}
	}
	@Override public boolean canTakeStackFromSlot(ItemStack stack) {
		if (stack.isEmpty()) return false;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			return this.inv.extract(this.slot, ItemResource.of(stack), 1, tx) > 0;
		}
	}
	@Override public ItemStack decreaseStackInSlot() {
		var resource = this.inv.getResource(this.slot);
		if (resource.isEmpty()) return ItemStack.EMPTY;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			int count = this.inv.extract(this.slot, resource, 1, tx);
			tx.commit();
			return resource.toStack(count);
		}
	}
	@Override public ItemStack getStackInSlot() { return this.inv.getResource(this.slot).toStack(this.inv.getAmountAsInt(this.slot)); }
}
