package forestry.agriculture.client;

import forestry.api.client.IClientModuleHandler;
import forestry.agriculture.features.MultifarmMenuTypes;
import forestry.agriculture.multifarm.gui.MultifarmScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class MultifarmClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(MultifarmClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MultifarmMenuTypes.FARM.menuType(), MultifarmScreen::new);
	}
}
