package forestry.core.platform.inventory;

import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.core.platform.util.InventoryData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import forestry.api.core.ITransactionalContainer;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryPlain implements ITransactionalContainer, INbtWritable, INbtReadable {

	private final NonNullList<ItemStack> contents;
	private final String name;
	private final int stackLimit;
	private final List<Consumer<int[]>> commitListeners = new ArrayList<>();
	private final InventoryTransferHandler transferHandler = new InventoryTransferHandler(this, slots -> {
		setChanged();
		for (var listener : this.commitListeners) listener.accept(slots);
	});

	public void addCommitListener(Consumer<int[]> listener) { this.commitListeners.add(listener); }

	@Override
	public ResourceHandler<ItemResource> getTransferHandler() { return this.transferHandler; }

	public InventoryPlain(int size, String name, int stackLimit) {
		this.contents = NonNullList.withSize(size, ItemStack.EMPTY);
		this.name = name;
		this.stackLimit = stackLimit;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack stack : this.contents) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public NonNullList<ItemStack> getContents() {
		return this.contents;
	}

	@Override
	public int getContainerSize() {
		return this.contents.size();
	}

	@Override
	public ItemStack getItem(int slotId) {
		return this.contents.get(slotId);
	}

	@Override
	public ItemStack removeItem(int slotId, int count) {
		ItemStack itemStack = this.contents.get(slotId);
		if (itemStack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		return itemStack.split(count);
	}

	@Override
	public void setItem(int slotId, ItemStack itemstack) {
        this.contents.set(slotId, itemstack);
	}

	@Override
	public int getMaxStackSize() {
		return this.stackLimit;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player PlayerEntity) {
		return false;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slotIndex) {
		ItemStack removed = this.contents.get(slotIndex);
		this.contents.set(slotIndex, ItemStack.EMPTY);
		return removed;
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return true;
	}

	/* INBTagable */
	@Override
	public void read(CompoundTag CompoundNBT, HolderLookup.Provider registries) {
		InventoryData.read(this, this.name, CompoundNBT, registries);
	}

	@Override
	public CompoundTag write(CompoundTag CompoundNBT, HolderLookup.Provider registries) {
		InventoryData.write(this, this.name, CompoundNBT, registries);
		return CompoundNBT;
	}

	/* Fields */
	@Override
	public void clearContent() {
        this.contents.clear();
	}
}
