package forestry.apiculture.alveary;

import forestry.apiculture.alveary.multiblock.AlvearySieveBlockEntity;

import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.platform.inventory.watchers.ISlotPickupWatcher;
import forestry.core.content.resources.EnumCraftingMaterial;
import forestry.core.platform.util.ItemStackUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AlvearySieveInventory extends InventoryAdapterTile<AlvearySieveBlockEntity> implements ISlotPickupWatcher {
	public static final int SLOT_POLLEN_1 = 0;
	public static final int SLOTS_POLLEN_COUNT = 4;
	public static final int SLOT_SIEVE = 4;

	public AlvearySieveInventory(AlvearySieveBlockEntity alvearySieve) {
		super(alvearySieve, 5, "Items", 1);
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("woven_silk")));
	}

	public boolean canStorePollen() {
		if (getItem(SLOT_SIEVE).isEmpty()) {
			return false;
		}

		for (int i = SLOT_POLLEN_1; i < SLOT_POLLEN_1 + SLOTS_POLLEN_COUNT; i++) {
			if (getItem(i).isEmpty()) {
				return true;
			}
		}

		return false;
	}

	public void storePollenStack(ItemStack itemstack) {
		for (int i = SLOT_POLLEN_1; i < SLOT_POLLEN_1 + SLOTS_POLLEN_COUNT; i++) {
			if (getItem(i).isEmpty()) {
				setItem(i, itemstack);
				return;
			}
		}
	}

	/* ISlotPickupWatcher */
	@Override
	public void onTake(int slotIndex, Player player) {
		if (slotIndex == SLOT_SIEVE) {
			for (int i = SLOT_POLLEN_1; i < SLOT_POLLEN_1 + SLOTS_POLLEN_COUNT; i++) {
				setItem(i, ItemStack.EMPTY);
			}
		} else {
			setItem(SLOT_SIEVE, ItemStack.EMPTY);
		}
	}
}
