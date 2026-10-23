package forestry.api.core.backpacks;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Use @SubscribeEvent on a method taking this event as an argument. Will fire whenever a backpack tries to store an item. Processing will stop if the stacksize
 * of stackToStow drops to 0 or less or the event is canceled.
 */
public class BackpackStowEvent extends BackpackEvent {
	public final ItemStack stackToStow;

	public BackpackStowEvent(Player player, IBackpackDefinition backpackDefinition, Container backpackInventory, ItemStack stackToStow) {
		super(player, backpackDefinition, backpackInventory);
		this.stackToStow = stackToStow;
	}
}
