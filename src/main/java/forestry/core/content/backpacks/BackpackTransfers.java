package forestry.core.content.backpacks;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/** Tops up existing main/offhand stacks; never fills an empty slot or equipment slot. */
public final class BackpackTransfers {
	private BackpackTransfers() {
	}

	public static int topOff(Player player, SlottedStorage<ItemVariant> source, int sourceSlot, int limit, TransactionContext parent) {
		ItemVariant resource = source.getSlot(sourceSlot).getResource();
		if (resource.isBlank() || limit <= 0) return 0;
		var target = PlayerInventoryStorage.of(player);
		int moved = 0;
		for (int i = 0; i <= Inventory.INVENTORY_SIZE && moved < limit; i++) {
			int slot = i == Inventory.INVENTORY_SIZE ? Inventory.SLOT_OFFHAND : i;
			if (!target.getSlot(slot).getResource().equals(resource)) continue;
			try (var transfer = Transaction.openNested(parent)) {
				int available = (int) Math.min(limit - moved, source.getSlot(sourceSlot).getAmount());
				int inserted = (int) target.getSlot(slot).insert(resource, available, transfer);
				if (inserted > 0 && source.getSlot(sourceSlot).extract(resource, inserted, transfer) == inserted) {
					transfer.commit();
					moved += inserted;
				}
			}
		}
		return moved;
	}
}
