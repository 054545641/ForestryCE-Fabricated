package forestry.core.platform.inventory;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.world.level.storage.ValueOutput;

import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.network.IStreamable;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.InventoryData;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * With permission from Krapht.
 */
public class InventoryAdapter implements IInventoryAdapter, IStreamable {

	private final InventoryPlain inventory;
	private boolean allowAutomation = true;
	private Runnable inventoryOwner = () -> {};

	public void setInventoryOwner(Runnable owner) { this.inventoryOwner = owner; }
	@Nullable
	private int[] slotMap;

	public InventoryAdapter(int size, String name) {
		this(size, name, 64);
	}

	public InventoryAdapter(int size, String name, int stackLimit) {
		this(new InventoryPlain(size, name, stackLimit));
	}

	public InventoryAdapter(InventoryPlain inventory) {
		this.inventory = inventory;
		inventory.addCommitListener(slots -> {
			for (int slot : slots) setItem(slot, getItem(slot));
			setChanged();
		});
		configureSided();
	}

	public InventoryAdapter disableAutomation() {
		this.allowAutomation = false;
		return this;
	}

	/**
	 * @return Copy of this inventory. Stacks are copies.
	 */
	public InventoryAdapter copy() {
		InventoryAdapter copy = new InventoryAdapter(this.inventory.getContainerSize(), "TEST_TITLE_PLEASE_IGNORE", this.inventory.getMaxStackSize());

		for (int i = 0; i < this.inventory.getContainerSize(); i++) {
			if (!this.inventory.getItem(i).isEmpty()) {
				copy.setItem(i, this.inventory.getItem(i).copy());
			}
		}

		return copy;
	}

	/* IINVENTORY */
	@Override
	public SlottedStorage<ItemVariant> getTransferHandler() {
		return this.inventory.getTransferHandler();
	}

	@Override
	public boolean isEmpty() {
		return this.inventory.isEmpty();
	}

	@Override
	public int getContainerSize() {
		return this.inventory.getContainerSize();
	}

	@Override
	public ItemStack getItem(int slotId) {
		return this.inventory.getItem(slotId);
	}

	@Override
	public ItemStack removeItem(int slotId, int count) {
		return this.inventory.removeItem(slotId, count);
	}

	@Override
	public void setItem(int slotId, ItemStack itemstack) {
        this.inventory.setItem(slotId, itemstack);
	}

	@Override
	public int getMaxStackSize() {
		return this.inventory.getMaxStackSize();
	}

	@Override
	public void setChanged() {
        this.inventory.setChanged();
		this.inventoryOwner.run();
	}

	@Override
	public ItemStack removeItemNoUpdate(int slotIndex) {
		return this.inventory.removeItemNoUpdate(slotIndex);
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return true;
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return true;
	}

	@Override
	public boolean isLocked(int slotIndex) {
		return false;
	}

	/* ISIDEDINVENTORY */
	@Override
	public int[] getSlotsForFace(Direction side) {
		if (this.allowAutomation && this.slotMap != null) {
			return this.slotMap;
		}
		return new int[0];
	}

	private void configureSided() {
		int count = getContainerSize();
        this.slotMap = new int[count];
		for (int i = 0; i < count; i++) {
            this.slotMap[i] = i;
		}
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return false;
	}

	/* SAVING & LOADING */
	@Override
	public void read(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		InventoryUtil.readFromNBT(this, this.inventory.getName(), compoundNBT, registries);
	}

	@Override
	public CompoundTag write(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		InventoryUtil.writeToNBT(this, this.inventory.getName(), compoundNBT, registries);
		return compoundNBT;
	}

	@Override
	public void write(ValueOutput output) {
		forestry.core.platform.util.InventoryData.write(this, this.inventory.getName(), output);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		InventoryData.writeStream(data, this.inventory);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		InventoryData.readStream(data, this.inventory);
	}

	/* FIELDS */

	@Override
	public void clearContent() {
        this.inventory.clearContent();
	}
}
