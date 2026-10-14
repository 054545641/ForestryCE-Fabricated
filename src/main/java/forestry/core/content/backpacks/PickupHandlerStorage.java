package forestry.core.content.backpacks;

import forestry.api.core.backpacks.IBackpackDefinition;
import forestry.core.content.backpacks.gui.ContainerBackpack;
import forestry.core.content.backpacks.gui.ContainerNaturalistBackpack;
import forestry.core.content.backpacks.items.BackpackItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PickupHandlerStorage {
	public static boolean onItemPickup(Player player, ItemEntity entityitem) {
		if (player.level().isClientSide() || entityitem.hasPickUpDelay() || (entityitem.getTarget() != null && !entityitem.getTarget().equals(player.getUUID()))) {
			return false;
		}

		ItemStack itemstack = entityitem.getItem();
		if (itemstack.isEmpty()) {
			return false;
		}

		// Do not pick up if a backpack is open
		if (player.containerMenu instanceof ContainerBackpack || player.containerMenu instanceof ContainerNaturalistBackpack) {
			return false;
		}

		// Make sure to top off manually placed itemstacks in player inventory first
		topOffPlayerInventory(player, itemstack);

		for (ItemStack pack : player.getInventory().getNonEquipmentItems()) {
			if (itemstack.isEmpty()) {
				break;
			}

			if (pack.isEmpty() || !(pack.getItem() instanceof BackpackItem backpack)) {
				continue;
			}

			IBackpackDefinition backpackDefinition = backpack.getDefinition();
			if (backpackDefinition.getFilter().test(itemstack)) {
				BackpackItem.tryStowing(player, pack, itemstack);
			}
		}

		return itemstack.isEmpty();
	}

	/**
	 * This tops off existing stacks in the player's inventory. That way you can keep f.e. a stack of dirt or cobblestone in your inventory which gets refreshed
	 * constantly by picked up items.
	 */
	private static void topOffPlayerInventory(Player player, ItemStack itemstack) {
		var staging = new net.minecraft.world.SimpleContainer(itemstack.copy());
		try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
			int moved = BackpackTransfers.topOff(player, net.neoforged.neoforge.transfer.item.VanillaContainerWrapper.of(staging), 0, itemstack.getCount(), transaction);
			transaction.commit();
			itemstack.shrink(moved);
		}
	}
}
