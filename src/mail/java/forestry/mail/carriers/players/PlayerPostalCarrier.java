package forestry.mail.carriers.players;

import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostOffice;
import forestry.api.mail.IPostalCarrier;
import forestry.api.mail.IPostalState;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.platform.util.PlayerUtil;
import forestry.mail.letters.MailAddress;
import forestry.mail.network.packets.PacketPOBoxInfoResponse;
import forestry.mail.letters.EnumDeliveryState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PlayerPostalCarrier implements IPostalCarrier {
	private final Identifier iconID;

	public PlayerPostalCarrier() {
		this.iconID = ForestryConstants.forestry("mail/carrier.player");
	}

	@Override
	public String getTranslationKey() {
		return "for.gui.addressee.player";
	}

	@Override
	public TextureAtlasSprite getSprite() {
		return IForestryClientApi.INSTANCE.getTextureManager().getSprite(this.iconID);
	}

	@Override
	public IPostalState deliverLetter(ServerLevel level, IPostOffice office, IMailAddress recipient, ItemStack letterStack, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
		return forestry.mail.postoffice.PostalTransactions.attempt(transaction, true, tx -> {
			var registry = POBoxRegistry.getOrCreate(level);
			if (!recipient.isValid() || !registry.isValidPOBox(recipient)) return EnumDeliveryState.NO_MAILBOX;
			POBox box = registry.getOrCreatePOBox(recipient, tx);
			if (!box.storeLetter(letterStack, tx)) return EnumDeliveryState.MAILBOX_FULL;
			new net.neoforged.neoforge.transfer.transaction.RootCommitJournal(() -> {
				ServerPlayer serverPlayer = level.getServer().getPlayerList().getPlayer(recipient.getPlayerProfile().id());
				if (serverPlayer != null) NetworkUtil.sendToPlayer(new PacketPOBoxInfoResponse(box.getPOBoxInfo(), false), serverPlayer);
			}).updateSnapshots(tx);
			return EnumDeliveryState.OK;
		});
	}

	@Override
	public IMailAddress getRecipient(MinecraftServer minecraftServer, String recipientName) {
		return minecraftServer.services().nameToIdCache().get(recipientName).map(profile -> new MailAddress(new com.mojang.authlib.GameProfile(profile.id(), profile.name()))).orElse(MailAddress.INVALID);
	}

	@Override
	public String toString() {
		return "player";
	}
}
