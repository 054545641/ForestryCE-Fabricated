package forestry.api.mail;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface IPostOffice {

	void collectPostage(NonNullList<ItemStack> stamps);

	/** All state changes must join the supplied transaction; notifications wait for root commit. */
	IPostalState lodgeLetter(ServerLevel world, ItemStack itemstack, TransactionContext transaction);

	default IPostalState lodgeLetter(ServerLevel world, ItemStack itemstack, boolean execute) {
		try (var tx = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			IPostalState state = lodgeLetter(world, itemstack, tx);
			if (execute && state.isOk()) tx.commit();
			return state;
		}
	}

	/**
	 * Used to withdraw collected stamps, taking the cheapest denomination the post office holds.
	 *
	 * @param max The most stamps to withdraw
	 * @return The withdrawn stamps, or an empty stack when the post office holds none
	 */
	ItemStack getAnyStamp(int max);

	/**
	 * Used to withdraw collected stamps of one exact item.
	 *
	 * @param stamp The stamp item to withdraw
	 * @param max   The most stamps to withdraw
	 * @return The withdrawn stamps, or an empty stack when the post office holds none of that item
	 */
	ItemStack getAnyStamp(Item stamp, int max);
}
