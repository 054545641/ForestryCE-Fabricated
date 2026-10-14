package forestry.api.mail;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import forestry.api.client.ITextureManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Postal Carriers are systems which can be hooked into Forestry's mail system to handle mail delivery.
 * <p>
 * The two available carriers in vanilla Forestry are
 * "player" - Delivers mail to individual players.
 * "trader" - Handles mail addressed to trade stations.
 */
public interface IPostalCarrier {
	/**
	 * @return The translation key for the human-readable name for this carrier.
	 */
	String getTranslationKey();

	/**
	 * Sprite registered to the Gui Texture Map at {@link ITextureManager}.
	 * Must be 26 pixels wide and 15 pixels tall. The texture itself must be 32x32 square.
	 */
	TextureAtlasSprite getSprite();

	/**
	 * Handle delivery of a letter addressed to this carrier.
	 *
	 * @param world       The world the {@link IPostOffice} handles.
	 * @param office      {link @IPostOffice} which received this letter and handed it to the carrier.
	 * @param recipient   An identifier for the recipient as typed by the player into the address field.
	 * @param letterstack ItemStack representing the letter. See {@code forestry.mail.letters.LetterUtils} for helper functions to validate and extract it.
	 * @param transaction The transaction shared with the sender, recipient inventories and collected postage.
	 * @return {link IPostalState} holding information on success or failure for delivery.
	 */
	IPostalState deliverLetter(ServerLevel world, IPostOffice office, IMailAddress recipient, ItemStack letterstack, TransactionContext transaction);

	default IPostalState deliverLetter(ServerLevel world, IPostOffice office, IMailAddress recipient, ItemStack letterstack, boolean execute) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			IPostalState state = deliverLetter(world, office, recipient, letterstack, tx);
			if (execute && state.isOk()) tx.commit();
			return state;
		}
	}

	IMailAddress getRecipient(MinecraftServer minecraftServer, String recipientName);
}
