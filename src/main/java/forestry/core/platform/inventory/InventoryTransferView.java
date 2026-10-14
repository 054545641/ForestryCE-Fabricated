package forestry.core.platform.inventory;

import forestry.api.core.ITransactionalContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntUnaryOperator;

/** Slot mapping and automation policy only; never owns a second snapshot. */
public final class InventoryTransferView implements ResourceHandler<ItemResource> {
	private final ResourceHandler<ItemResource> owner;
	private final int size;
	private final IntUnaryOperator slots;
	private final BiPredicate<Integer, ItemResource> insert;
	private final BiPredicate<Integer, ItemResource> extract;

	public InventoryTransferView(ResourceHandler<ItemResource> owner, int size, IntUnaryOperator slots,
		BiPredicate<Integer, ItemResource> insert, BiPredicate<Integer, ItemResource> extract) {
		this.owner = owner;
		this.size = size;
		this.slots = slots;
		this.insert = insert;
		this.extract = extract;
	}

	public static ResourceHandler<ItemResource> internal(Container inventory) {
		return inventory instanceof ITransactionalContainer transactional ? transactional.getTransferHandler() : VanillaContainerWrapper.of(inventory);
	}

	public static ResourceHandler<ItemResource> automation(Container inventory, @Nullable Direction side) {
		// A null side is still automation. For worldly inventories use the union of
		// permitted faces, so it cannot bypass disabled automation or output rules.
		return new InventoryTransferView(internal(inventory), inventory.getContainerSize(), i -> i,
			(i, resource) -> allowed(inventory, side, i, resource, true),
			(i, resource) -> allowed(inventory, side, i, resource, false));
	}

	private static boolean allowed(Container inventory, @Nullable Direction side, int slot, ItemResource resource, boolean inserting) {
		if (!(inventory instanceof WorldlyContainer worldly)) return !inserting || inventory.canPlaceItem(slot, resource.toStack());
		for (Direction face : side == null ? Direction.values() : new Direction[]{side}) {
			for (int exposed : worldly.getSlotsForFace(face)) {
				if (exposed == slot && (inserting
					? inventory.canPlaceItem(slot, resource.toStack()) && worldly.canPlaceItemThroughFace(slot, resource.toStack(), face)
					: worldly.canTakeItemThroughFace(slot, resource.toStack(), face))) return true;
			}
		}
		return false;
	}

	private int slot(int index) { return this.slots.applyAsInt(Objects.checkIndex(index, this.size)); }
	@Override public int size() { return this.size; }
	@Override public ItemResource getResource(int index) { return this.owner.getResource(slot(index)); }
	@Override public long getAmountAsLong(int index) { return this.owner.getAmountAsLong(slot(index)); }
	@Override public long getCapacityAsLong(int index, ItemResource resource) {
		return resource.isEmpty() || isValid(index, resource) ? this.owner.getCapacityAsLong(slot(index), resource) : 0;
	}
	@Override public boolean isValid(int index, ItemResource resource) {
		return this.insert.test(slot(index), resource) && this.owner.isValid(slot(index), resource);
	}
	@Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return isValid(index, resource) ? this.owner.insert(slot(index), resource, amount, transaction) : 0;
	}
	@Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		return this.extract.test(slot(index), resource) ? this.owner.extract(slot(index), resource, amount, transaction) : 0;
	}
}
