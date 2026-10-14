package forestry.mail.carriers.trading;

import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.api.mail.*;
import forestry.mail.letters.MailAddress;
import forestry.mail.letters.EnumDeliveryState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TradeStationCarrier implements IPostalCarrier {
	private final Identifier iconID;

	public TradeStationCarrier() {
		this.iconID = ForestryConstants.forestry("mail/carrier.trader");
	}

	@Override
	public String getTranslationKey() {
		return "for.gui.addressee.trader";
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public TextureAtlasSprite getSprite() {
		return IForestryClientApi.INSTANCE.getTextureManager().getSprite(this.iconID);
	}

	@Override
	public IPostalState deliverLetter(ServerLevel world, IPostOffice office, IMailAddress recipient, ItemStack letterStack, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
		ITradeStation trade = TradeStationRegistry.getOrCreate(world).getTradeStation(recipient);
		if (trade == null || !trade.isValid()) {
			return EnumDeliveryState.NO_MAILBOX;
		}

		return trade.handleLetter(world, recipient, letterStack, transaction);
	}

	@Override
	public IMailAddress getRecipient(MinecraftServer minecraftServer, String recipientName) {
		return new MailAddress(recipientName);
	}

	@Override
	public String toString() {
		return "trader";
	}
}
