package forestry.api.mail;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface ILetterHandler {
	/** All state changes must join the supplied transaction; notifications wait for root commit. */
	IPostalState handleLetter(ServerLevel world, IMailAddress recipient, ItemStack letterStack, TransactionContext transaction);

	default IPostalState handleLetter(ServerLevel world, IMailAddress recipient, ItemStack letterStack, boolean execute) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			IPostalState state = handleLetter(world, recipient, letterStack, tx);
			if (execute && state.isOk()) tx.commit();
			return state;
		}
	}
}
