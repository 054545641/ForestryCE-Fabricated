package forestry.core.platform.util;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

public final class InventoryData {
	public static void writeStream(net.minecraft.network.RegistryFriendlyByteBuf buffer, Container inventory) {
		buffer.writeVarInt(inventory.getContainerSize());
		for (int i = 0; i < inventory.getContainerSize(); i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, inventory.getItem(i));
	}

	public static void readStream(net.minecraft.network.RegistryFriendlyByteBuf buffer, Container inventory) {
		int size = buffer.readVarInt();
		if (size != inventory.getContainerSize()) throw new IllegalArgumentException("Inventory size mismatch: " + size);
		ItemStack[] stacks = new ItemStack[size];
		for (int i = 0; i < size; i++) stacks[i] = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
		for (int i = 0; i < size; i++) inventory.setItem(i, stacks[i]);
	}

	public static void replaceStacks(Collection<ItemStack> destination, ListTag list, HolderLookup.Provider registries) {
		var stacks = new ArrayList<ItemStack>(list.size());
		for (var entry : list) {
			if (!(entry instanceof CompoundTag stackTag)) {
				throw new IllegalArgumentException("Expected an item stack compound");
			}
			ItemStack stack = readStack(stackTag, registries);
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
		}
		// a failed decode must leave pending products intact
		destination.clear();
		destination.addAll(stacks);
	}

	public static void read(Container inventory, String name, CompoundTag tag, HolderLookup.Provider registries) {
		ItemStack[] stacks = new ItemStack[inventory.getContainerSize()];
		Arrays.fill(stacks, ItemStack.EMPTY);
		boolean[] seen = new boolean[stacks.length];
		if (tag.contains(name) && !(tag.get(name) instanceof ListTag)) throw new IllegalArgumentException("Invalid inventory list: " + name);
		for (var entry : tag.getListOrEmpty(name)) {
			if (!(entry instanceof CompoundTag stackTag)) throw new IllegalArgumentException("Invalid inventory entry");
			int slot = stackTag.getIntOr("Slot", -1);
			if (slot < 0 || slot >= stacks.length || seen[slot]) throw new IllegalArgumentException("Invalid or duplicate inventory slot: " + slot);
			seen[slot] = true;
			stacks[slot] = readStack(stackTag, registries);
		}

		// decode everything before replacing live slots, including slots omitted from the save
		for (int slot = 0; slot < stacks.length; slot++) {
			inventory.setItem(slot, stacks[slot]);
		}
	}

	public static void write(Container inventory, String name, CompoundTag tag, HolderLookup.Provider registries) {
		ListTag stacks = new ListTag();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				CompoundTag stackTag = writeStack(stack, registries);
				// database inventories exceed the range of a byte
				stackTag.putInt("Slot", slot);
				stacks.add(stackTag);
			}
		}
		tag.put(name, stacks);
	}

	public static void write(Container inventory, String name, ValueOutput output) {
		var entries = output.childrenList(name);
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty()) {
				var entry = entries.addChild();
				entry.store(MapCodec.assumeMapUnsafe(ItemStack.OPTIONAL_CODEC), stack);
				entry.putInt("Slot", slot);
			}
		}
	}

	public static void writeStacks(Iterable<ItemStack> stacks, String name, ValueOutput output) {
		var entries = output.list(name, ItemStack.OPTIONAL_CODEC);
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				entries.add(stack);
			}
		}
	}

	public static ItemStack readStack(CompoundTag tag, HolderLookup.Provider registries) {
		return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
	}

	@Nullable
	public static ItemStack readNullableStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
		if (!tag.contains(key)) {
			return null;
		}
		ItemStack stack = readStack(tag.getCompound(key).orElseThrow(() -> new IllegalArgumentException("Invalid stack field " + key)), registries);
		return stack.isEmpty() ? null : stack;
	}

	public static CompoundTag writeStack(ItemStack stack, HolderLookup.Provider registries) {
		return (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
	}
}
