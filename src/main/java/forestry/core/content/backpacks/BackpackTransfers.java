package forestry.core.content.backpacks;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Tops up existing main/offhand stacks; never fills an empty slot or equipment slot. */
public final class BackpackTransfers {
	private BackpackTransfers() {
	}

	public static int topOff(Player player, ResourceHandler<ItemResource> source, int sourceSlot, int limit, TransactionContext parent) {
		ItemResource resource = source.getResource(sourceSlot);
		if (resource.isEmpty() || limit <= 0) return 0;
		var target = PlayerInventoryWrapper.of(player);
		int moved = 0;
		for (int i = 0; i <= Inventory.INVENTORY_SIZE && moved < limit; i++) {
			int slot = i == Inventory.INVENTORY_SIZE ? Inventory.SLOT_OFFHAND : i;
			if (!target.getResource(slot).equals(resource)) continue;
			try (var transfer = Transaction.open(parent)) {
				int available = Math.min(limit - moved, source.getAmountAsInt(sourceSlot));
				int inserted = target.insert(slot, resource, available, transfer);
				if (inserted > 0 && source.extract(sourceSlot, resource, inserted, transfer) == inserted) {
					transfer.commit();
					moved += inserted;
				}
			}
		}
		return moved;
	}
}
