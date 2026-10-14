package forestry.agriculture.client;

import forestry.api.client.IClientModuleHandler;
import forestry.agriculture.features.MinifarmMenuTypes;
import forestry.agriculture.minifarm.gui.MinifarmScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class MinifarmClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(MinifarmClientHandler::registerMenuScreens);
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(MinifarmMenuTypes.PLANTER.menuType(), MinifarmScreen::new);
	}
}
