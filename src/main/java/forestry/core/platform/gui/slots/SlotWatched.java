package forestry.core.platform.gui.slots;

import forestry.core.platform.inventory.watchers.FakeSlotChangeWatcher;
import forestry.core.platform.inventory.watchers.FakeSlotPickupWatcher;
import forestry.core.platform.inventory.watchers.ISlotChangeWatcher;
import forestry.core.platform.inventory.watchers.ISlotPickupWatcher;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Slot with a watcher callbacks.
 */
public class SlotWatched extends SlotForestry {
	private ISlotPickupWatcher pickupWatcher = FakeSlotPickupWatcher.INSTANCE;
	private ISlotChangeWatcher changeWatcher = FakeSlotChangeWatcher.INSTANCE;

	public SlotWatched(Container inventory, int slotIndex, int xPos, int yPos) {
		super(inventory, slotIndex, xPos, yPos);
	}

	public SlotWatched setPickupWatcher(ISlotPickupWatcher pickupWatcher) {
		this.pickupWatcher = pickupWatcher;
		return this;
	}

	public SlotWatched setChangeWatcher(ISlotChangeWatcher changeWatcher) {
		this.changeWatcher = changeWatcher;
		return this;
	}

	@Override
	public void onTake(Player player, ItemStack itemStack) {
		super.onTake(player, itemStack);
		// todo why is this commented out? (it was my doing)
		//if (player instanceof ServerPlayer) {
		//pickupWatcher.onTake(getSlotIndex(), player);
		//}
	}

	@Override
	public void setChanged() {
		super.setChanged();
        this.changeWatcher.onSlotChanged(this.container, getContainerSlot());
	}
}
