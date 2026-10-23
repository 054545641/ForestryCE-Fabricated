package forestry.environment;

import forestry.core.platform.inventory.*;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.core.platform.util.InventoryData;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.content.sorting.inventory.ItemHandlerFilter;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import io.netty.buffer.Unpooled;
import java.util.List;

public final class N3ItemProbe {
	private static final class Machine extends InventoryAdapterRestricted {
		int changes;
		int slotCallbacks;
		Machine() { super(3, "Items", 16); }
		@Override public boolean canSlotAccept(int slot, ItemStack stack) { return slot == 0 && stack.is(Items.APPLE); }
		@Override public int[] getSlotsForFace(Direction side) {
			if (super.getSlotsForFace(side).length == 0) return new int[0];
			return side == Direction.UP ? new int[]{0} : side == Direction.DOWN ? new int[]{1} : new int[0];
		}
		@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1 && side == Direction.DOWN; }
		@Override public void setChanged() { super.setChanged(); this.changes++; }
		@Override public void setItem(int slot, ItemStack stack) { super.setItem(slot, stack); this.slotCallbacks++; }
	}

	public static void items(GameTestHelper helper) {
		var apple = ItemResource.of(Items.APPLE);
		var stone = ItemResource.of(Items.STONE);
		var machine = new Machine();
		int[] dirtyOwner = {0};
		machine.setInventoryOwner(() -> dirtyOwner[0]++);
		var top = InventoryTransferView.automation(machine, Direction.UP);
		var bottom = InventoryTransferView.automation(machine, Direction.DOWN);
		var unsided = InventoryTransferView.automation(machine, null);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(top.insert(0, apple, 20, tx) == 16, "Machine slot capacity is enforced");
			helper.assertTrue(top.insert(1, apple, 1, tx) == 0 && top.insert(0, stone, 1, tx) == 0, "Side and input filters reject insertion");
			helper.assertTrue(top.extract(0, apple, 1, tx) == 0, "Input is not externally extractable");
			helper.assertTrue(machine.changes == 0 && machine.slotCallbacks == 0 && dirtyOwner[0] == 0, "Speculation never notifies machine or slot hooks");
			try (var nested = Transaction.open(tx)) {
				machine.getTransferHandler().extract(0, apple, 10, nested);
				machine.getTransferHandler().insert(1, stone, 8, nested);
				bottom.extract(1, stone, 3, nested);
				nested.commit();
			}
		}
		helper.assertTrue(machine.isEmpty() && machine.changes == 0 && machine.slotCallbacks == 0, "Shared internal and side journals restore together");
		try (var tx = Transaction.openRoot()) {
			top.insert(0, apple, 6, tx);
			machine.getTransferHandler().insert(1, stone, 5, tx);
			tx.commit();
		}
		helper.assertTrue(machine.changes == 1 && dirtyOwner[0] == 1 && machine.slotCallbacks == 2, "Commit marks persisted owner once and runs changed slot hooks");
		try (var tx = Transaction.openRoot()) {
			bottom.extract(1, stone, 2, tx);
			machine.getTransferHandler().insert(1, stone, 2, tx);
			tx.commit();
		}
		helper.assertTrue(machine.changes == 1, "Net zero does not dirty the inventory");
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(unsided.insert(1, stone, 1, tx) == 0 && unsided.extract(0, apple, 1, tx) == 0, "Null side cannot bypass automation rules");
			helper.assertTrue(unsided.extract(1, stone, 2, tx) == 2, "Null side exposes the union of allowed faces");
		}
		machine.disableAutomation();
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(top.insert(0, apple, 1, tx) == 0 && unsided.extract(1, stone, 1, tx) == 0, "Already-created side views respect disabled automation");
		}
		var range = new InventoryMapper(machine, 1, 2, false);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(InventoryUtil.tryAddStack(range, new ItemStack(Items.STONE, 20), true), "Internal range may fill product slots");
			helper.assertTrue(machine.getItem(1).getCount() == 16 && machine.getItem(2).getCount() == 9, "Slot mapping and per-slot limit are preserved");
			try (var child = Transaction.open(tx)) {
				machine.getTransferHandler().extract(1, stone, 6, child);
				child.commit();
			}
		}
		helper.assertTrue(machine.getItem(1).getCount() == 5 && machine.getItem(2).isEmpty() && machine.changes == 1, "Mapped and direct operations share rollback");

		int[] tileChanges = {0};
		var tile = new net.minecraft.world.level.block.entity.BlockEntity(net.minecraft.world.level.block.entity.BlockEntityTypes.CHEST,
			net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()) {
			@Override public void setChanged() { tileChanges[0]++; }
		};
		var tileInventory = new InventoryAdapterTile<>(tile, 2, "Items");
		try (var tx = Transaction.openRoot()) { tileInventory.getTransferHandler().insert(apple, 1, tx); }
		helper.assertTrue(tileChanges[0] == 0, "Tile-backed rollback does not notify block entity");
		InventoryUtil.tryAddStack(tileInventory, new ItemStack(Items.APPLE), true);
		helper.assertTrue(tileChanges[0] == 1, "Production tile adapter dirties block entity once on commit");

		var target = new InventoryPlain(2, "Items", 16);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(InventoryUtil.moveItemStack(machine.getTransferHandler(), target.getTransferHandler()), "Transfer uses the shared inventory transaction");
		}
		helper.assertTrue(target.isEmpty() && machine.getItem(0).getCount() == 6, "Transfer rollback conserves both inventories");
		helper.assertTrue(InventoryUtil.moveItemStack(machine.getTransferHandler(), target.getTransferHandler()) && target.getItem(0).getCount() == 6, "Committed transfer persists");
		ItemStack named = new ItemStack(Items.APPLE, 3);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Component-distinct"));
		helper.assertTrue(InventoryUtil.tryAddStack(target, named, true) && target.getItem(1).getCount() == 3, "Different components never merge");
		ItemStack read = target.getTransferHandler().getResource(1).toStack(3);
		read.set(DataComponents.CUSTOM_NAME, Component.literal("Unrelated"));
		helper.assertTrue(target.getItem(1).getHoverName().getString().equals("Component-distinct"), "Resource reads do not expose mutable stacks");
		int before = target.getItem(0).getCount();
		var batch = NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.APPLE, 2), new ItemStack(Items.DIAMOND));
		helper.assertTrue(!InventoryUtil.tryAddStacksCopy(target, batch, 0, 2, true) && target.getItem(0).getCount() == before, "Failed output batch rolls back earlier additions");
		helper.assertTrue(!InventoryUtil.tryAddStack(target, new ItemStack(Items.APPLE, 30), true) && target.getItem(0).getCount() == before, "All-or-nothing respects total capacity");
		var required = NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.APPLE, 4), new ItemStack(Items.APPLE, 4));
		helper.assertTrue(!InventoryUtil.deleteExactSet(target, required) && target.getItem(0).getCount() == before, "Duplicate requirements cannot spend the same stock twice");
		helper.assertTrue(InventoryUtil.removeSets(target, 2, List.of(new ItemStack(Items.APPLE)), null, false, false, true), "Set count is independent of number of ingredient kinds");
		helper.assertTrue(target.getItem(0).getCount() == 4, "Exact stacks are consumed first");
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(InventoryUtil.consumeIngredients(target, List.of(Ingredient.of(Items.APPLE)), null, false, false, true), "Ingredient consumption joins parent transaction");
		}
		helper.assertTrue(target.getItem(0).getCount() == 4, "Ingredient consumption rolls back with parent");

		var remainder = new InventoryPlain(2, "Items", 64);
		remainder.setItem(0, new ItemStack(Items.HONEY_BOTTLE, 2));
		remainder.setItem(1, new ItemStack(Items.STONE, 64));
		helper.assertTrue(!InventoryUtil.consumeIngredients(remainder, List.of(Ingredient.of(Items.HONEY_BOTTLE)), null, true, false, true), "No room for crafting remainder rejects the whole craft");
		helper.assertTrue(remainder.getItem(0).getCount() == 2, "Rejected remainder restores inputs");
		remainder.setItem(1, ItemStack.EMPTY);
		helper.assertTrue(InventoryUtil.consumeIngredients(remainder, List.of(Ingredient.of(Items.HONEY_BOTTLE)), null, true, false, false) && remainder.getItem(1).isEmpty(), "Simulation rolls back remainder insertion");
		helper.assertTrue(InventoryUtil.consumeIngredients(remainder, List.of(Ingredient.of(Items.HONEY_BOTTLE)), null, true, false, true) && remainder.getItem(1).is(Items.GLASS_BOTTLE), "Craft commits input and remainder together");
		var loose = new ItemStack(Items.STONE, 16);
		var stowing = new InventoryPlain(1, "Items", 16);
		try (var tx = Transaction.openRoot()) { helper.assertTrue(InventoryUtil.stowInInventory(loose, stowing, true), "Exact-fill stowing succeeds"); }
		helper.assertTrue(stowing.isEmpty() && loose.getCount() == 16, "Loose stack and destination both roll back");

		var routes = new InventoryPlain(6, "Items", 16);
		routes.setItem(Direction.NORTH.get3DDataValue(), new ItemStack(Items.APPLE, 15));
		var filter = new ItemHandlerFilter(routes.getTransferHandler(), stack -> List.of(Direction.NORTH, Direction.SOUTH));
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(filter.insert(apple, 8, tx) == 8, "Routing splits the remaining amount across buffers");
			helper.assertTrue(routes.getItem(Direction.SOUTH.get3DDataValue()).getCount() == 7, "Second route receives only the remainder, without duplication");
		}
		helper.assertTrue(routes.getItem(Direction.NORTH.get3DDataValue()).getCount() == 15 && routes.getItem(Direction.SOUTH.get3DDataValue()).isEmpty(), "Routing rollback restores every buffer");
		var mapped = new InventoryMapper(routes, Direction.SOUTH.get3DDataValue(), 1);
		helper.assertTrue(mapped.isEmpty(), "Mapped emptiness ignores other slots");
		mapped.clearContent();
		helper.assertTrue(routes.getItem(Direction.NORTH.get3DDataValue()).getCount() == 15, "Clearing a range leaves unrelated slots intact");
		helper.succeed();
	}

	public static void persistence(GameTestHelper helper) {
		var inventory = new InventoryAdapter(300, "Items", 64);
		ItemStack named = new ItemStack(Items.APPLE, 5);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Saved transactional item"));
		InventoryUtil.tryAddStack(inventory, named, 299, 1, true);
		CompoundTag saved = inventory.write(new CompoundTag(), helper.getLevel().registryAccess());
		try {
			var path = java.nio.file.Files.createTempFile("forestry-n3-items-", ".dat");
			try {
				NbtIo.writeCompressed(saved, path);
				var restored = new InventoryAdapter(300, "Items", 64);
				restored.read(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()), helper.getLevel().registryAccess());
				helper.assertTrue(ItemStack.matches(restored.getItem(299), named), "Compressed save/reopen preserves components and int slot index");
				restored.setItem(0, new ItemStack(Items.DIAMOND));
				restored.read(saved, helper.getLevel().registryAccess());
				helper.assertTrue(restored.getItem(0).isEmpty(), "Repeated load replaces missing slots");
			} finally { java.nio.file.Files.deleteIfExists(path); }
		} catch (java.io.IOException e) { throw new RuntimeException(e); }
		var validation = new InventoryAdapter(300, "Items", 64);
		validation.read(saved, helper.getLevel().registryAccess());
		CompoundTag duplicate = saved.copy();
		var entries = duplicate.getListOrEmpty("Items");
		entries.add(entries.getFirst().copy());
		boolean rejectedDuplicate = false;
		try { validation.read(duplicate, helper.getLevel().registryAccess()); } catch (IllegalArgumentException expected) { rejectedDuplicate = true; }
		helper.assertTrue(rejectedDuplicate && ItemStack.matches(validation.getItem(299), named), "Duplicate slots are rejected without partial replacement");
		CompoundTag invalidIndex = saved.copy();
		invalidIndex.getListOrEmpty("Items").getCompoundOrEmpty(0).putInt("Slot", 300);
		boolean rejectedIndex = false;
		try { validation.read(invalidIndex, helper.getLevel().registryAccess()); } catch (IllegalArgumentException expected) { rejectedIndex = true; }
		helper.assertTrue(rejectedIndex && ItemStack.matches(validation.getItem(299), named), "Out-of-range inventory slots are rejected");
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
		try {
			inventory.writeData(buffer);
			var restored = new InventoryAdapter(300, "Items", 64);
			restored.readData(buffer);
			helper.assertTrue(ItemStack.matches(restored.getItem(299), named), "Inventory stream preserves components");
			buffer.clear();
			buffer.writeVarInt(300);
			ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, new ItemStack(Items.STONE));
			boolean rejected = false;
			try { restored.readData(buffer); } catch (RuntimeException expected) { rejected = true; }
			helper.assertTrue(rejected && restored.getItem(0).isEmpty() && ItemStack.matches(restored.getItem(299), named), "Truncated sync is rejected before overwriting any slot");
		} finally { buffer.release(); }
		helper.succeed();
	}
}
