package forestry.core.platform.inventory;

import com.google.common.base.Preconditions;
import net.minecraft.world.entity.ContainerUser;
import forestry.core.features.CoreDataComponents;
import forestry.api.core.IFilterSlotDelegate;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.NBTUtilForestry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import forestry.api.core.ITransactionalContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.Random;

public abstract class ItemInventory implements ITransactionalContainer, IFilterSlotDelegate {
	private static final String KEY_SLOTS = "Slots";
	private static final String KEY_UID = "UID";
	private static final Random rand = new Random();

	private final ResourceHandler<ItemResource> itemHandler = new InventoryTransferHandler(this, slots -> setChanged());

	protected final Player player;
	private ItemStack parent;    //TODO not final any more. Is this a problem
	protected final NonNullList<ItemStack> inventoryStacks;

	public ItemInventory(Player player, int size, ItemStack parent) {
		Preconditions.checkArgument(!parent.isEmpty(), "Parent cannot be empty.");

		this.player = player;
		this.parent = parent;
		this.inventoryStacks = NonNullList.withSize(size, ItemStack.EMPTY);

		ensureInventoryId(parent);
		loadInventoryStacks(parent);
	}

	protected void ensureInventoryId(ItemStack parent) {
		DataComponentType<Integer> uidComponent = getInventoryUidComponent();
		if (uidComponent != null) {
			if (!parent.has(uidComponent)) {
				parent.set(uidComponent, newInventoryId());
			}
			return;
		}

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			nbt = new CompoundTag();
		}
		setUID(nbt); // Set a uid to identify the itemStack on SMP
		NBTUtilForestry.setItemStackTag(parent, nbt);
	}

	protected void loadInventoryStacks(ItemStack parent) {
		if (usesComponentInventoryStorage()) {
			parent.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.inventoryStacks);
			return;
		}

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			return;
		}
		CompoundTag nbtSlots = nbt.getCompoundOrEmpty(KEY_SLOTS);
		for (int i = 0; i < this.inventoryStacks.size(); i++) {
			String slotKey = getSlotNBTKey(i);
			if (nbtSlots.contains(slotKey)) {
				CompoundTag itemNbt = nbtSlots.getCompoundOrEmpty(slotKey);
				ItemStack itemStack = InventoryUtil.deserializeItemStack(itemNbt, this.player.registryAccess());
				this.inventoryStacks.set(i, itemStack);
			} else {
				this.inventoryStacks.set(i, ItemStack.EMPTY);
			}
		}
	}

	public static int getOccupiedSlotCount(ItemStack itemStack) {
		ItemContainerContents container = itemStack.get(DataComponents.CONTAINER);
		if (container != null) {
			return (int) container.nonEmptyItemCopyStream().count();
		}

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(itemStack);
		if (nbt == null) {
			return 0;
		}

		CompoundTag slotNbt = nbt.getCompoundOrEmpty(KEY_SLOTS);
		return slotNbt.size();
	}

	protected static int newInventoryId() {
		return rand.nextInt();
	}

	private void setUID(CompoundTag nbt) {
		if (!nbt.contains(KEY_UID)) {
			nbt.putInt(KEY_UID, newInventoryId());
		}
	}

	public boolean isParentItemInventory(ItemStack itemStack) {
		ItemStack parent = getParent();
		return isSameItemInventory(parent, itemStack);
	}

	protected ItemStack getParent() {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = this.player.getItemInHand(hand);
			if (isSameItemInventory(held, this.parent)) {
				return held;
			}
		}
		return this.parent;
	}

	protected void setParent(ItemStack parent) {
		this.parent = parent;
	}

	@Nullable
	protected InteractionHand getHand() {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = this.player.getItemInHand(hand);
			if (isSameItemInventory(held, this.parent)) {
				return hand;
			}
		}
		return null;
	}

	protected boolean isSameItemInventory(ItemStack base, ItemStack comparison) {
		if (base.isEmpty() || comparison.isEmpty()) {
			return false;
		}

		if (base.getItem() != comparison.getItem()) {
			return false;
		}

		DataComponentType<Integer> uidComponent = getInventoryUidComponent();
		if (uidComponent != null) {
			Integer baseUid = base.get(uidComponent);
			Integer comparisonUid = comparison.get(uidComponent);
			return baseUid != null && baseUid.equals(comparisonUid);
		}

		CompoundTag baseTagCompound = NBTUtilForestry.getItemStackTag(base);
		CompoundTag comparisonTagCompound = NBTUtilForestry.getItemStackTag(comparison);
		if (baseTagCompound == null || comparisonTagCompound == null) {
			return false;
		}

		if (!baseTagCompound.contains(KEY_UID) || !comparisonTagCompound.contains(KEY_UID)) {
			return false;
		}

		return baseTagCompound.getInt(KEY_UID).filter(id -> comparisonTagCompound.getInt(KEY_UID).filter(id::equals).isPresent()).isPresent();
	}

	private void writeToParentNBT() {
		ItemStack parent = getParent();
		writeInventoryToParent(parent);
	}

	protected void writeInventoryToParent(ItemStack parent) {
		if (usesComponentInventoryStorage()) {
			parent.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.inventoryStacks));
			return;
		}

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			nbt = new CompoundTag();
		}

		CompoundTag slotsNbt = new CompoundTag();
		for (int i = 0; i < getContainerSize(); i++) {
			ItemStack itemStack = getItem(i);
			if (!itemStack.isEmpty()) {
				String slotKey = getSlotNBTKey(i);
				CompoundTag itemNbt = InventoryUtil.serializeItemStack(itemStack, this.player.registryAccess());
				slotsNbt.put(slotKey, itemNbt);
			}
		}

		nbt.put(KEY_SLOTS, slotsNbt);
		onWriteNBT(nbt);
		NBTUtilForestry.setItemStackTag(parent, nbt);
	}

	private static String getSlotNBTKey(int i) {
		return Integer.toString(i, Character.MAX_RADIX);
	}

	protected void onWriteNBT(CompoundTag nbt) {
	}

	protected DataComponentType<Integer> getInventoryUidComponent() {
		return CoreDataComponents.ITEM_INVENTORY_UID.get();
	}

	protected boolean usesComponentInventoryStorage() {
		return true;
	}

	public void onSlotClick(int slotIndex, Player player) {
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.inventoryStacks) {
			if (!itemstack.isEmpty()) {
				return false;
			}
		}

		return true;
	}


	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack itemstack = ContainerHelper.removeItem(this.inventoryStacks, index, count);

		if (!itemstack.isEmpty()) {
			this.setChanged();
		}

		return itemstack;
	}

	@Override
	public void setItem(int index, ItemStack itemstack) {
		this.inventoryStacks.set(index, itemstack);

		ItemStack parent = getParent();
		writeSlotToParent(parent, index, itemstack);
	}

	@Override
	public void setItem(int index, ItemStack stack, boolean insideTransaction) {
		if (insideTransaction) this.inventoryStacks.set(index, stack);
		else setItem(index, stack);
	}

	@Override
	public ResourceHandler<ItemResource> getTransferHandler() { return this.itemHandler; }

	protected void writeSlotToParent(ItemStack parent, int index, ItemStack itemstack) {
		if (usesComponentInventoryStorage()) {
			writeInventoryToParent(parent);
			return;
		}

		CompoundTag nbt = NBTUtilForestry.getItemStackTag(parent);
		if (nbt == null) {
			nbt = new CompoundTag();
		}

		CompoundTag slotNbt;
		if (!nbt.contains(KEY_SLOTS)) {
			slotNbt = new CompoundTag();
			nbt.put(KEY_SLOTS, slotNbt);
		} else {
			slotNbt = nbt.getCompoundOrEmpty(KEY_SLOTS);
		}

		String slotKey = getSlotNBTKey(index);

		if (itemstack.isEmpty()) {
			slotNbt.remove(slotKey);
		} else {
			slotNbt.put(slotKey, InventoryUtil.serializeItemStack(itemstack, this.player.registryAccess()));
		}
		NBTUtilForestry.setItemStackTag(parent, nbt);
	}

	@Override
	public ItemStack getItem(int i) {
		return this.inventoryStacks.get(i);
	}

	@Override
	public int getContainerSize() {
		return this.inventoryStacks.size();
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public final void setChanged() {
		writeToParentNBT();
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int slotIndex, ItemStack itemStack) {
		return canSlotAccept(slotIndex, itemStack);
	}

	@Override
	public void startOpen(ContainerUser user) {
	}

	@Override
	public void stopOpen(ContainerUser user) {
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack toReturn = getItem(slot);

		if (!toReturn.isEmpty()) {
			setItem(slot, ItemStack.EMPTY);
		}

		return toReturn;
	}

	/* IFilterSlotDelegate */
	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return true;
	}

	@Override
	public boolean isLocked(int slotIndex) {
		return false;
	}

	/* Fields */

	public ResourceHandler<ItemResource> getItemHandler() {
		return InventoryTransferView.automation(this, null);
	}

	@Override
	public void clearContent() {
		this.inventoryStacks.clear();
	}
}
