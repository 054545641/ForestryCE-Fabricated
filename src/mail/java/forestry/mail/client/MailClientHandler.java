package forestry.mail.client;

import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import forestry.api.ForestryConstants;
import forestry.api.client.IClientModuleHandler;
import forestry.mail.features.MailMenuTypes;
import forestry.mail.gui.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class MailClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(MailClientHandler::registerItemColors);
		modBus.addListener(MailClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MailMenuTypes.CATALOGUE.menuType(), CatalogScreen::new);
		event.register(MailMenuTypes.LETTER.menuType(), LetterScreen::new);
		event.register(MailMenuTypes.MAILBOX.menuType(), MailboxScreen::new);
		event.register(MailMenuTypes.STAMP_COLLECTOR.menuType(), StampCollectorScreen::new);
		event.register(MailMenuTypes.TRADE_NAME.menuType(), TradeStationNamingScreen::new);
		event.register(MailMenuTypes.TRADER.menuType(), TradeStationScreen::new);
	}

	private static void registerItemColors(RegisterColorHandlersEvent.ItemTintSources event) {
		event.register(ForestryConstants.forestry("mail_stamp"), StampTintSource.CODEC);
	}

}
