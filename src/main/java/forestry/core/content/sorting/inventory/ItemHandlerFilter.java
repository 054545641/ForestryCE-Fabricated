package forestry.core.content.sorting.inventory;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** A virtual input routes into the real directional buffers in the caller's transaction. */
public final class ItemHandlerFilter implements ResourceHandler<ItemResource> {
	private final ResourceHandler<ItemResource> inventory;
	private final Function<ItemStack, List<Direction>> directions;
	public ItemHandlerFilter(ResourceHandler<ItemResource> inventory, Function<ItemStack, List<Direction>> directions) {
		this.inventory = inventory;
		this.directions = directions;
	}
	@Override public int size() { return 1; }
	@Override public ItemResource getResource(int index) { Objects.checkIndex(index, 1); return ItemResource.EMPTY; }
	@Override public long getAmountAsLong(int index) { Objects.checkIndex(index, 1); return 0; }
	@Override public long getCapacityAsLong(int index, ItemResource resource) {
		Objects.checkIndex(index, 1);
		if (resource.isEmpty()) return 64;
		long capacity = 0;
		for (Direction direction : this.directions.apply(resource.toStack())) capacity += this.inventory.getCapacityAsLong(direction.get3DDataValue(), resource);
		return capacity;
	}
	@Override public boolean isValid(int index, ItemResource resource) {
		Objects.checkIndex(index, 1);
		return !this.directions.apply(resource.toStack()).isEmpty();
	}
	@Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		int inserted = 0;
		for (Direction direction : this.directions.apply(resource.toStack(amount))) {
			inserted += this.inventory.insert(direction.get3DDataValue(), resource, amount - inserted, transaction);
			if (inserted == amount) break;
		}
		return inserted;
	}
	@Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		Objects.checkIndex(index, 1);
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return 0;
	}
}
