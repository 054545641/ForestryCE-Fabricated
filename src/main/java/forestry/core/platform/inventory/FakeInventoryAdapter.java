package forestry.core.platform.inventory;

import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.minecraft.world.level.storage.ValueOutput;

import forestry.api.core.IInventoryAdapter;
import net.minecraft.world.entity.ContainerUser;
import forestry.core.platform.config.Constants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum FakeInventoryAdapter implements IInventoryAdapter {
	INSTANCE;

	@Override
	public ResourceHandler<ItemResource> getTransferHandler() {
		return VanillaContainerWrapper.of(this);
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return false;
	}

	@Override
	public boolean isLocked(int slotIndex) {
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return Constants.SLOTS_NONE;
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return false;
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, Direction direction) {
		return false;
	}

	@Override
	public int getContainerSize() {
		return 0;
	}

	@Override
	public ItemStack getItem(int p_70301_1_) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
	}

	@Override
	public int getMaxStackSize() {
		return 0;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void startOpen(ContainerUser user) {
	}

	@Override
	public void stopOpen(ContainerUser user) {
	}

	@Override
	public boolean canPlaceItem(int p_94041_1_, ItemStack p_94041_2_) {
		return false;
	}

	@Override
	public void read(CompoundTag compoundNBT, HolderLookup.Provider registries) {
	}

	@Override
	public CompoundTag write(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		return compoundNBT;
	}

	@Override
	public void write(ValueOutput output) {
	}

	@Override
	public void clearContent() {
	}
}
