package forestry.api.mail;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public interface ILetterHandler {
	/** All state changes must join the supplied transaction; notifications wait for root commit. */
	IPostalState handleLetter(ServerLevel world, IMailAddress recipient, ItemStack letterStack, TransactionContext transaction);

	default IPostalState handleLetter(ServerLevel world, IMailAddress recipient, ItemStack letterStack, boolean execute) {
		// Compatibility entry point for callers using simulation. New callers pass their transaction explicitly.
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			IPostalState state = handleLetter(world, recipient, letterStack, tx);
			if (execute && state.isOk()) tx.commit();
			return state;
		}
	}
}
