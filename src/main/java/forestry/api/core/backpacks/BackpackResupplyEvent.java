package forestry.api.core.backpacks;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

/**
 * Use @SubscribeEvent on a method taking this event as an argument. Will fire whenever a backpack tries to resupply to a player inventory. Processing will stop
 * if the event is canceled.
 */
public class BackpackResupplyEvent extends BackpackEvent {
	public BackpackResupplyEvent(Player player, IBackpackDefinition backpackDefinition, Container backpackInventory) {
		super(player, backpackDefinition, backpackInventory);
	}
}
