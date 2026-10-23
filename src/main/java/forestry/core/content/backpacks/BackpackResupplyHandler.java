package forestry.core.content.backpacks;

import forestry.api.core.backpacks.BackpackResupplyEvent;
import forestry.core.platform.inventory.ItemInventory;
import forestry.core.content.backpacks.inventory.BackpackInventory;
import forestry.core.content.backpacks.items.BackpackItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;


public class BackpackResupplyHandler {
	private static NonNullList<ItemStack> getBackpacks(Inventory playerInventory) {
		NonNullList<ItemStack> backpacks = NonNullList.create();
		for (ItemStack itemStack : playerInventory.getNonEquipmentItems()) {
			if (itemStack.getItem() instanceof BackpackItem) {
				backpacks.add(itemStack);
			}
		}
		return backpacks;
	}

	public static void resupply(Player player) {
		// Do not attempt resupplying if this backpack is already opened.
		if (!player.level().isClientSide() && player.containerMenu instanceof InventoryMenu) {
			for (ItemStack backpack : getBackpacks(player.getInventory())) {
				if (BackpackItem.getMode(backpack) == BackpackMode.RESUPPLY) {
					// Load their inventory
					BackpackItem backpackItem = (BackpackItem) backpack.getItem();
					ItemInventory backpackInventory = new BackpackInventory(player, backpackItem.getBackpackSize(), backpack);

					BackpackResupplyEvent event = new BackpackResupplyEvent(player, backpackItem.getDefinition(), backpackInventory);
					if (!forestry.api.core.backpacks.BackpackEvent.dispatch(event).isCanceled()) {
						try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openNested(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.getCurrentUnsafe())) {
							for (int i = 0; i < backpackInventory.getContainerSize(); i++) {
								if (BackpackTransfers.topOff(player, backpackInventory.getTransferHandler(), i, 1, transaction) > 0) break;
							}
							transaction.commit();
						}
					}
				}
			}
		}
	}

}
