package forestry.core.platform.compat.curios.client;

import forestry.api.client.IClientModuleHandler;
import forestry.core.features.CoreItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class CuriosClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(CuriosClientHandler::clientSetup);
	}

	private static void clientSetup(FMLClientSetupEvent event) {
		ICurioRenderer.register(CoreItems.SPECTACLES.item(), SpectaclesCurioRenderer::new);
	}
}
