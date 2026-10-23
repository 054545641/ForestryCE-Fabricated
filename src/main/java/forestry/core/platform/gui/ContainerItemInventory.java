package forestry.core.platform.gui;

import forestry.core.platform.gui.slots.SlotLocked;
import forestry.core.platform.inventory.ItemInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public abstract class ContainerItemInventory<I extends ItemInventory> extends ContainerForestry {

	protected final I inventory;

	protected ContainerItemInventory(int containerId, I inventory, Inventory playerInventory, int xInv, int yInv, MenuType<?> type) {
		super(containerId, type, playerInventory.player);
		this.inventory = inventory;

		addPlayerInventory(playerInventory, xInv, yInv);
	}

	@Override
	protected void addHotbarSlot(Inventory playerInventory, int slot, int x, int y) {
		ItemStack stackInSlot = playerInventory.getItem(slot);

		if (this.inventory.isParentItemInventory(stackInSlot)) {
			addSlot(new SlotLocked(playerInventory, slot, x, y));
		} else {
			addSlot(new Slot(playerInventory, slot, x, y));
		}
	}

	@Override
	protected final boolean canAccess(Player player) {
		return stillValid(player);
	}

	@Override
	public final boolean stillValid(Player PlayerEntity) {
		return this.inventory.stillValid(PlayerEntity);
	}

	@Override
	public void clicked(int slotId, int button, ContainerInput clickTypeIn, Player player) {
		super.clicked(slotId, button, clickTypeIn, player);

		if (stillValid(player) && slotId >= 0 && slotId < this.slots.size()) {
            this.inventory.onSlotClick(this.slots.get(slotId).getContainerSlot(), player);
		}
	}

	public I getItemInventory() {
		return this.inventory;
	}

}
