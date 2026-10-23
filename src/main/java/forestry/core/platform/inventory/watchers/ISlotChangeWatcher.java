package forestry.core.platform.inventory.watchers;

import net.minecraft.world.Container;

public interface ISlotChangeWatcher {
	void onSlotChanged(Container inventory, int slot);
}
