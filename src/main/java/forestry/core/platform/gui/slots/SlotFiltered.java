package forestry.core.platform.gui.slots;

import forestry.api.client.ForestrySprites;
import forestry.api.core.IFilterSlotDelegate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Slot which only takes specific items, specified by the IFilterSlotDelegate.
 */
public class SlotFiltered extends SlotWatched {
	private final IFilterSlotDelegate filterSlotDelegate;
	private Identifier blockedTexture = guiSprite(ForestrySprites.SLOT_BLOCKED);
	@Nullable private Identifier backgroundTexture;

	public <T extends Container & IFilterSlotDelegate> SlotFiltered(T inventory, int slotIndex, int xPos, int yPos) {
		super(inventory, slotIndex, xPos, yPos);
		this.filterSlotDelegate = inventory;
	}

	@Override
	public boolean mayPlace(ItemStack itemstack) {
		int slotIndex = getContainerSlot();
		return !this.filterSlotDelegate.isLocked(slotIndex) &&
			(itemstack.isEmpty() || this.filterSlotDelegate.canSlotAccept(slotIndex, itemstack));
	}

    private static Identifier guiSprite(Identifier sprite) {
        return sprite.getNamespace().equals("forestry") && sprite.getPath().startsWith("slots/") ? sprite.withPrefix("container/") : sprite;
    }

	public SlotFiltered setBlockedSprite(Identifier sprite) {
		this.blockedTexture = guiSprite(sprite);
		return this;
	}

	public SlotFiltered setBackgroundSprite(Identifier sprite) {
		this.backgroundTexture = guiSprite(sprite);
		return this;
	}

	// Empty slot icons are resolved from the GUI sprite atlas.
	@Nullable
	@Override
	public Identifier getNoItemIcon() {
		return !mayPlace(getItem()) ? this.blockedTexture : this.backgroundTexture;
	}
}
