package forestry.core.content.energy.client;

import forestry.core.content.energy.features.EnergyMenus;
import forestry.core.content.energy.screen.BiogasEngineScreen;
import forestry.core.content.energy.screen.CombustionEngineScreen;
import forestry.core.content.energy.screen.PeatEngineScreen;
import forestry.core.content.energy.screen.SolarEngineScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class EnergyClientHandler implements forestry.api.client.IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(EnergyClientHandler::registerMenuScreens);
	}


	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(EnergyMenus.ENGINE_BIOGAS.menuType(), BiogasEngineScreen::new);
		event.register(EnergyMenus.ENGINE_COMBUSTION.menuType(), CombustionEngineScreen::new);
		event.register(EnergyMenus.ENGINE_PEAT.menuType(), PeatEngineScreen::new);
		event.register(EnergyMenus.ENGINE_SOLAR.menuType(), SolarEngineScreen::new);
	}
}
