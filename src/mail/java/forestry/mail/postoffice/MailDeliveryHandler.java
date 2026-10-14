package forestry.mail.postoffice;

import forestry.api.mail.IPostalState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/** Virtual input: one accepted item means one delivery in the caller's transaction. */
public final class MailDeliveryHandler implements ResourceHandler<ItemResource> {
	private final Predicate<ItemStack> valid;
	private final BiFunction<ItemStack, TransactionContext, IPostalState> deliver;
	public MailDeliveryHandler(Predicate<ItemStack> valid, BiFunction<ItemStack, TransactionContext, IPostalState> deliver) {
		this.valid = valid;
		this.deliver = deliver;
	}
	@Override public int size() { return 1; }
	@Override public ItemResource getResource(int index) { Objects.checkIndex(index, 1); return ItemResource.EMPTY; }
	@Override public long getAmountAsLong(int index) { Objects.checkIndex(index, 1); return 0; }
	@Override public long getCapacityAsLong(int index, ItemResource resource) {
		Objects.checkIndex(index, 1);
		return resource.isEmpty() || isValid(index, resource) ? 1 : 0;
	}
	@Override public boolean isValid(int index, ItemResource resource) {
		Objects.checkIndex(index, 1);
		return this.valid.test(resource.toStack());
	}
	@Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (amount == 0 || !isValid(index, resource)) return 0;
		return PostalTransactions.attempt(transaction, true, tx -> this.deliver.apply(resource.toStack(1), tx)).isOk() ? 1 : 0;
	}
	@Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}
}
