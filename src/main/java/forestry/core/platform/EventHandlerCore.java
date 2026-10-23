package forestry.core.platform;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.ISpeciesType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ForestryConstants.MOD_ID)
public class EventHandlerCore {
	@SubscribeEvent
	public static void handlePlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		Player player = event.getEntity();
		syncBreedingTrackers(player);
	}

	@SubscribeEvent
	public static void handlePlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player player = event.getEntity();
		syncBreedingTrackers(player);
	}

	private static void syncBreedingTrackers(Player player) {
		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			IBreedingTracker breedingTracker = type.getBreedingTracker(player.level(), player.getGameProfile());
			breedingTracker.syncToPlayer(player);
		}
	}
}
