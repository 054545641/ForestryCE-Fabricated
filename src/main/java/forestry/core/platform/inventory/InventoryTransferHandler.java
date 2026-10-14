package forestry.core.platform.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** One journal per owning inventory. Views must delegate to this handler. */
public final class InventoryTransferHandler extends SnapshotJournal<List<ItemStack>> implements ResourceHandler<ItemResource> {
	private final Container inventory;
	private final Consumer<int[]> committed;

	public InventoryTransferHandler(Container inventory, Consumer<int[]> committed) {
		this.inventory = inventory;
		this.committed = committed;
	}

	@Override
	public int size() { return this.inventory.getContainerSize(); }
	@Override
	public ItemResource getResource(int index) { return ItemResource.of(this.inventory.getItem(Objects.checkIndex(index, size()))); }
	@Override
	public long getAmountAsLong(int index) { return this.inventory.getItem(Objects.checkIndex(index, size())).getCount(); }
	@Override
	public long getCapacityAsLong(int index, ItemResource resource) {
		Objects.checkIndex(index, size());
		return resource.isEmpty() ? this.inventory.getMaxStackSize() : Math.min(this.inventory.getMaxStackSize(resource.toStack()), resource.getMaxStackSize());
	}
	@Override
	public boolean isValid(int index, ItemResource resource) { Objects.checkIndex(index, size()); return true; }
	@Override
	public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		ItemResource current = getResource(index);
		if (!current.isEmpty() && !current.equals(resource)) return 0;
		int inserted = Math.min(amount, Math.max(0, getCapacityAsInt(index, resource) - getAmountAsInt(index)));
		if (inserted > 0) {
			updateSnapshots(transaction);
			this.inventory.setItem(index, resource.toStack(getAmountAsInt(index) + inserted), true);
		}
		return inserted;
	}
	@Override
	public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
		TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
		if (!getResource(index).equals(resource)) return 0;
		int extracted = Math.min(amount, getAmountAsInt(index));
		if (extracted > 0) {
			updateSnapshots(transaction);
			this.inventory.setItem(index, resource.toStack(getAmountAsInt(index) - extracted), true);
		}
		return extracted;
	}
	@Override
	protected List<ItemStack> createSnapshot() {
		List<ItemStack> snapshot = new ArrayList<>(size());
		for (int i = 0; i < size(); i++) snapshot.add(this.inventory.getItem(i).copy());
		return snapshot;
	}
	@Override
	protected void revertToSnapshot(List<ItemStack> snapshot) {
		for (int i = 0; i < snapshot.size(); i++) this.inventory.setItem(i, snapshot.get(i), true);
	}
	@Override
	protected void onRootCommit(List<ItemStack> original) {
		int[] changed = java.util.stream.IntStream.range(0, size())
			.filter(i -> !ItemStack.matches(original.get(i), this.inventory.getItem(i))).toArray();
		if (changed.length > 0) this.committed.accept(changed);
	}
}
