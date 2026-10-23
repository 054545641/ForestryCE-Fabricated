package forestry.mail.postoffice;

import forestry.api.mail.IPostalState;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/** Virtual input: one accepted item means one delivery in the caller's transaction. */
public final class MailDeliveryHandler implements SingleSlotStorage<ItemVariant> {
    private final Predicate<ItemStack> valid;
    private final BiFunction<ItemStack, TransactionContext, IPostalState> deliver;
    public MailDeliveryHandler(Predicate<ItemStack> valid, BiFunction<ItemStack, TransactionContext, IPostalState> deliver) {
        this.valid = valid; this.deliver = deliver;
    }
    @Override public ItemVariant getResource() { return ItemVariant.blank(); }
    @Override public boolean isResourceBlank() { return true; }
    @Override public long getAmount() { return 0; }
    @Override public long getCapacity() { return 1; }
    @Override public boolean supportsExtraction() { return false; }
    @Override public long insert(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount);
        if (amount == 0 || !valid.test(resource.toStack())) return 0;
        return PostalTransactions.attempt(transaction, true, tx -> deliver.apply(resource.toStack(1), tx)).isOk() ? 1 : 0;
    }
    @Override public long extract(ItemVariant resource, long amount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, amount); return 0;
    }
}
