package forestry.core.platform.gui.slots;

import forestry.api.client.ForestrySprites;
import forestry.api.core.IFilterSlotDelegate;
import net.minecraft.world.Container;

public class SlotLiquidIn extends SlotFiltered {
	public <T extends Container & IFilterSlotDelegate> SlotLiquidIn(T inventory, int slotIndex, int xPos, int yPos) {
		super(inventory, slotIndex, xPos, yPos);
		setBackgroundSprite(ForestrySprites.SLOT_LIQUID);
	}
}
