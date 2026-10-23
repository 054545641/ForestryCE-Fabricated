package forestry.mail.postoffice;

import forestry.api.mail.IPostalState;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A failed delivery and a simulation both discard all nested postal operations. */
public final class PostalTransactions {
	private PostalTransactions() {}

	public static IPostalState attempt(@Nullable TransactionContext parent, boolean execute,
		Function<TransactionContext, IPostalState> delivery) {
		try (var transaction = Transaction.openNested(parent)) {
			IPostalState state = delivery.apply(transaction);
			if (execute && state.isOk()) transaction.commit();
			return state;
		}
	}
}
