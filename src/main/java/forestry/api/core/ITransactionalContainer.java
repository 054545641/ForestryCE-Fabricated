package forestry.api.core;

import net.minecraft.world.Container;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

public interface ITransactionalContainer extends Container {
	/**
	 * Gets the shared handler for internal inventory operations, without automation filters.
	 * The handler owns rollback and commit notifications for the persisted inventory.
	 * Delegating containers must return that handler (or a slot view), never wrap themselves.
	 *
	 * @return The handler owned by the backing inventory
	 */
	SlottedStorage<ItemVariant> getTransferHandler();
    /** Only the backing participant calls this; notifications belong to final commit. */
    default void setItemInTransaction(int slot, net.minecraft.world.item.ItemStack stack) { setItem(slot, stack); }
}
