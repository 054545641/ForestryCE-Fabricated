package forestry.api.core;

import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public interface ITransactionalContainer extends Container {
	/**
	 * Gets the shared handler for internal inventory operations, without automation filters.
	 * The handler owns rollback and commit notifications for the persisted inventory.
	 * Delegating containers must return that handler (or a slot view), never wrap themselves.
	 *
	 * @return The handler owned by the backing inventory
	 */
	ResourceHandler<ItemResource> getTransferHandler();
}
