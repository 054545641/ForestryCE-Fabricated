package forestry.core.platform.util;

import net.minecraft.core.HolderLookup;
import forestry.core.engine.circuits.ISocketable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import forestry.core.platform.inventory.InventoryTransferView;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import forestry.core.platform.transfer.CommitAction;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;

import javax.annotation.Nullable;
import java.util.List;

public abstract class InventoryUtil {
	/**
	 * Attempts to move an ItemStack from one inventory to another.
	 *
	 * @param source The source IInventory.
	 * @param dest   The destination IInventory.
	 * @return true if any items were moved
	 */
	public static boolean moveItemStack(Storage<ItemVariant> source, Storage<ItemVariant> dest) {
        if (source == dest) return false;
        try (var tx = transaction()) {
            for (var view : source.nonEmptyViews()) {
                var variant = view.getResource();
                long moved = StorageUtil.move(source, dest, variant::equals, Math.min(view.getAmount(), variant.toStack().getMaxStackSize()), tx);
                if (moved > 0) { tx.commit(); return true; }
            }
            return false;
        }
	}

	/**
	 * Attempts to move an ItemStack from one inventory to another.
	 *
	 * @param source       The source IInventory.
	 * @param destinations The destination IInventory.
	 * @return true if any items were moved
	 */
	public static boolean moveItemStack(Storage<ItemVariant> source, Iterable<Storage<ItemVariant>> destinations) {
		for (Storage<ItemVariant> dest : destinations) {
			if (moveItemStack(source, dest)) {
				return true;
			}
		}
		return false;
	}

	/** Shared inventory entry point, also used by fluid/container exchanges. */
	public static net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage<ItemVariant> getTransferHandler(Container inventory) { return InventoryTransferView.internal(inventory); }

	private static Transaction transaction() { return Transaction.openNested(Transaction.getCurrentUnsafe()); }

	public static boolean consumeIngredients(Container inventory, List<Ingredient> ingredients, @Nullable Player player, boolean stowContainer, boolean craftingTools, boolean doRemove) {
		try (var tx = transaction()) {
			int[] counts = InventoryCrafting.createConsume(ingredients, inventory, craftingTools);
			if (counts.length == 0 && !ingredients.isEmpty()) return false;
			if (!consumeItems(inventory, counts, player, stowContainer, tx)) return false;
			if (doRemove) tx.commit();
			return true;
		}
	}

	private record Remainder(int slot, ItemStack stack) {}

	private static boolean consumeItems(Container inventory, int[] counts, @Nullable Player player, boolean stowContainer, TransactionContext tx) {
		var handler = getTransferHandler(inventory);
		var remainders = new java.util.ArrayList<Remainder>();
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] <= 0) continue;
			ItemStack stack = inventory.getItem(i).copy();
			if (stack.isEmpty() || handler.getSlot(i).extract(ItemVariant.of(stack), counts[i], tx) != counts[i]) return false;
			if (stowContainer) {
				var remainder = stack.getCraftingRemainder();
				if (remainder != null) {
					for (int n = 0; n < counts[i]; n++) remainders.add(new Remainder(i, remainder.create()));
				}
			}
		}
		for (var remainder : remainders) {
			if (!stowRemainder(remainder.stack(), inventory, remainder.slot(), player, tx)) return false;
		}
		return true;
	}

	public static boolean removeSets(Container inventory, int count, List<ItemStack> set, @Nullable Player player, boolean stowContainer, boolean craftingTools, boolean doRemove) {
		try (var tx = transaction()) {
			if (removeSets(inventory, count, set, player, stowContainer, craftingTools) == null) return false;
			if (doRemove) tx.commit();
			return true;
		}
	}

	@Nullable
	public static NonNullList<ItemStack> removeSets(Container inventory, int count, List<ItemStack> set, @Nullable Player player, boolean stowContainer, boolean craftingTools) {
		if (count < 0) throw new IllegalArgumentException("Negative set count");
		try (var tx = transaction()) {
			int[] counts = new int[inventory.getContainerSize()];
			NonNullList<ItemStack> removed = NonNullList.withSize(set.size(), ItemStack.EMPTY);
			for (int requirement = 0; requirement < set.size(); requirement++) {
				ItemStack wanted = set.get(requirement);
				int remaining = Math.multiplyExact(wanted.getCount(), count);
				int total = remaining;
				for (int pass = 0; pass < 2 && remaining > 0; pass++) {
					for (int slot = 0; slot < counts.length && remaining > 0; slot++) {
						ItemStack offered = inventory.getItem(slot);
						if (!offered.isEmpty() && (pass == 0 ? ItemStack.isSameItemSameComponents(wanted, offered) : InventoryCrafting.isCraftingEquivalent(wanted, offered, craftingTools))) {
							int take = Math.min(remaining, offered.getCount() - counts[slot]);
							counts[slot] += take;
							remaining -= take;
							if (take > 0) removed.set(requirement, offered.copyWithCount(total));
						}
					}
				}
				if (remaining > 0) return null;
			}
			if (!consumeItems(inventory, counts, player, stowContainer, tx)) return null;
			tx.commit();
			return removed;
		}
	}

	public static boolean deleteExactSet(Container inventory, NonNullList<ItemStack> required) {
		try (var tx = transaction()) {
			var items = getTransferHandler(inventory);
			for (ItemStack stack : required) {
				if (!stack.isEmpty() && items.extract(ItemVariant.of(stack), stack.getCount(), tx) != stack.getCount()) return false;
			}
			tx.commit();
			return true;
		}
	}

	/* CONTAINS */

	public static boolean contains(Container inventory, List<ItemStack> query) {
		return contains(inventory, query, 0, inventory.getContainerSize());
	}

	public static boolean contains(Container inventory, List<ItemStack> query, int startSlot, int slots) {
		Container range = new forestry.core.platform.inventory.wrappers.InventoryMapper(inventory, startSlot, slots, false);
		return removeSets(range, 1, query, null, false, false, false);
	}

	public static boolean isEmpty(Container inventory, int slotStart, int slotCount) {
		for (int i = slotStart; i < slotStart + slotCount; i++) {
			if (!inventory.getItem(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public static NonNullList<ItemStack> getStacks(Container inventory) {
		NonNullList<ItemStack> stacks = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			stacks.set(i, inventory.getItem(i));
		}
		return stacks;
	}

	public static NonNullList<ItemStack> getStacks(Container inventory, int slot1, int length) {
		NonNullList<ItemStack> result = NonNullList.withSize(length, ItemStack.EMPTY);
		for (int i = slot1; i < slot1 + length; i++) {
			result.set(i - slot1, inventory.getItem(i));
		}
		return result;
	}

	public static boolean tryAddStacksCopy(Container inventory, NonNullList<ItemStack> stacks, int startSlot, int slots, boolean all) {
		try (var tx = transaction()) {
			for (ItemStack stack : stacks) {
				if (!stack.isEmpty() && !tryAddStack(inventory, stack, startSlot, slots, all)) return false;
			}
			tx.commit();
			return true;
		}
	}

	public static boolean tryAddStack(Container inventory, ItemStack stack, boolean all) { return tryAddStack(inventory, stack, 0, inventory.getContainerSize(), all, true); }
	public static boolean tryAddStack(Container inventory, ItemStack stack, boolean all, boolean doAdd) { return tryAddStack(inventory, stack, 0, inventory.getContainerSize(), all, doAdd); }
	public static boolean tryAddStack(Container inventory, ItemStack stack, int startSlot, int slots, boolean all) { return tryAddStack(inventory, stack, startSlot, slots, all, true); }
	public static boolean tryAddStack(Container inventory, ItemStack stack, int startSlot, int slots, boolean all, boolean doAdd) {
		try (var tx = transaction()) {
			int added = insert(inventory, stack, startSlot, slots, tx);
			boolean success = all ? added == stack.getCount() : added > 0;
			if (success && doAdd) tx.commit();
			return success;
		}
	}

	public static int addStack(Container inventory, ItemStack stack, boolean doAdd) { return addStack(inventory, stack, 0, inventory.getContainerSize(), doAdd); }
	public static int addStack(Container inventory, ItemStack stack, int startSlot, int slots, boolean doAdd) {
		try (var tx = transaction()) {
			int added = insert(inventory, stack, startSlot, slots, tx);
			if (doAdd) tx.commit();
			return added;
		}
	}

	private static int insert(Container inventory, ItemStack stack, int start, int size, TransactionContext tx) {
		java.util.Objects.checkFromIndexSize(start, size, inventory.getContainerSize());
		if (stack.isEmpty()) return 0;
		var handler = getTransferHandler(inventory);
		if (inventory instanceof Inventory && start == 0 && size == inventory.getContainerSize()) {
			// The player wrapper excludes body/saddle slots. Its insertion also keeps
			// returned items in backpack slots instead of filling empty equipment slots.
			return Math.toIntExact(handler.insert(ItemVariant.of(stack), stack.getCount(), tx));
		}
		var view = new InventoryTransferView(handler, size, i -> start + i, (i, r) -> true, (i, r) -> true);
		return Math.toIntExact(StorageUtil.insertStacking(view.getSlots(), ItemVariant.of(stack), stack.getCount(), tx));
	}

	public static boolean stowInInventory(ItemStack stack, Container inventory, boolean doAdd) { return stowInInventory(stack, inventory, doAdd, 0, inventory.getContainerSize()); }
	public static boolean stowInInventory(ItemStack stack, Container inventory, boolean doAdd, int start, int slots) {
		try (var tx = transaction()) {
			int added = insert(inventory, stack, start, slots, tx);
			if (doAdd && added > 0) {
				// The caller's loose stack is also part of the transaction.
				new net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<ItemStack>() {
					@Override protected ItemStack createSnapshot() { return stack.copy(); }
					@Override protected void readSnapshot(ItemStack snapshot) { stack.setCount(snapshot.getCount()); }
				}.updateSnapshots(tx);
				stack.shrink(added);
				tx.commit();
			}
			return added > 0;
		}
	}

	private static boolean stowRemainder(ItemStack stack, Container inventory, int slot, @Nullable Player player, TransactionContext tx) {
		int added = insert(inventory, stack, slot, 1, tx);
		added += insert(inventory, stack.copyWithCount(stack.getCount() - added), 0, inventory.getContainerSize(), tx);
		if (added == stack.getCount()) return true;
		if (player == null) return false;
		ItemStack dropped = stack.copyWithCount(stack.getCount() - added);
		new CommitAction(() -> player.drop(dropped, true)).updateSnapshots(tx);
		return true;
	}

	public static void stowContainerItem(ItemStack stack, Container inventory, int slot, @Nullable Player player) {
		var remainder = stack.getCraftingRemainder();
		if (remainder == null) return;
		try (var tx = transaction()) {
			for (int i = 0; i < stack.getCount(); i++) {
				if (!stowRemainder(remainder.create(), inventory, slot, player, tx)) return;
			}
			tx.commit();
		}
	}

	public static void deepCopyInventoryContents(Container source, Container destination) {
		if (source.getContainerSize() != destination.getContainerSize()) {
			throw new IllegalArgumentException("Inventory sizes do not match. Source: " + source + ", Destination: " + destination);
		}

		for (int i = 0; i < source.getContainerSize(); i++) {
			ItemStack stack = source.getItem(i);
			if (!stack.isEmpty()) {
				stack = stack.copy();
			}
			destination.setItem(i, stack);
		}
	}

	public static void dropSockets(ISocketable socketable, Level level, double x, double y, double z) {
		for (int slot = 0; slot < socketable.getSocketCount(); slot++) {
			ItemStack itemstack = socketable.getSocket(slot);
			Containers.dropItemStack(level, x, y, z, itemstack);
			socketable.setSocket(slot, ItemStack.EMPTY);
		}
	}

	public static void dropSockets(ISocketable socketable, Level level, BlockPos pos) {
		dropSockets(socketable, level, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void readFromNBT(Container inventory, String name, CompoundTag tag, HolderLookup.Provider registries) {
		InventoryData.read(inventory, name, tag, registries);
	}

	public static void writeToNBT(Container inventory, String name, CompoundTag tag, HolderLookup.Provider registries) {
		InventoryData.write(inventory, name, tag, registries);
	}

	public static ItemStack deserializeItemStack(CompoundTag tag, HolderLookup.Provider registries) {
		return InventoryData.readStack(tag, registries);
	}

	public static CompoundTag serializeItemStack(ItemStack stack, HolderLookup.Provider registries) {
		return InventoryData.writeStack(stack, registries);
	}
}
