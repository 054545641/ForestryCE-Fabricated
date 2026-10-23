package forestry.mail.inventory;

import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.mail.letters.PostageUtil;
import forestry.mail.postoffice.StampCollectorBlockEntity;
import net.minecraft.world.item.ItemStack;

public class StampCollectorInventory extends InventoryAdapterTile<StampCollectorBlockEntity> {
	public static final short SLOT_FILTER = 0;
	public static final short SLOT_BUFFER_1 = 1;
	public static final short SLOT_BUFFER_COUNT = 27;

	public StampCollectorInventory(StampCollectorBlockEntity tile) {
		super(tile, 28, "INV");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return PostageUtil.isStamp(stack);
	}
}
