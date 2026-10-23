package forestry.core.platform;

import forestry.api.core.genetics.IBreedingTracker;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PickupHandlerCore {
	public static void onItemPickup(Player player, ItemEntity entity) {
		ItemStack stack = entity.getItem();

		if (!stack.isEmpty()) {
			IIndividualHandlerItem.ifPresent(stack, individual -> {
				IBreedingTracker tracker = individual.getType().getBreedingTracker(entity.level(), player.getGameProfile());
				tracker.registerPickup(individual.getSpecies());
			});
		}
	}
}
