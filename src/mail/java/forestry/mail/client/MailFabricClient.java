package forestry.mail.client;

import forestry.api.ForestryConstants;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.color.item.ItemTintSources;

public final class MailFabricClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ItemTintSources.ID_MAPPER.put(ForestryConstants.forestry("mail_stamp"), StampTintSource.CODEC);
        net.minecraft.client.gui.screens.MenuScreens.register(forestry.mail.features.MailMenuTypes.LETTER.menuType(), forestry.mail.gui.LetterScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(forestry.mail.features.MailMenuTypes.CATALOGUE.menuType(), forestry.mail.gui.CatalogScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(forestry.mail.features.MailMenuTypes.MAILBOX.menuType(), forestry.mail.gui.MailboxScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(forestry.mail.features.MailMenuTypes.STAMP_COLLECTOR.menuType(), forestry.mail.gui.StampCollectorScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(forestry.mail.features.MailMenuTypes.TRADE_NAME.menuType(), forestry.mail.gui.TradeStationNamingScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(forestry.mail.features.MailMenuTypes.TRADER.menuType(), forestry.mail.gui.TradeStationScreen::new);
        forestry.mail.network.packets.PacketPOBoxInfoResponse.installClientReceiver((info, silent) ->
            MailboxInfoToast.addOrUpdate(net.minecraft.client.Minecraft.getInstance().gui.toastManager(), info, silent));
    }
}
