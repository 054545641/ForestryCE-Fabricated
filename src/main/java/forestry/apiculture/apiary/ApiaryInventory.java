package forestry.apiculture.apiary;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeHousingInventory;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveFrame;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.apiculture.bees.BeeHousingInventory;
import forestry.core.platform.util.SlotUtil;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;

public class ApiaryInventory extends BeeHousingInventory implements IBeeHousingInventory {
	public static final int SLOT_FRAMES_1 = 9;
	public static final int SLOT_FRAMES_COUNT = 3;

	public ApiaryInventory() {
		super(12);
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (SlotUtil.isSlotInRange(slotIndex, SLOT_FRAMES_1, SLOT_FRAMES_COUNT)) {
			return stack.getItem() instanceof IHiveFrame && getItem(slotIndex).isEmpty();
		}

		return super.canSlotAccept(slotIndex, stack);
	}

	// override for pipe automation
	@Override
	public boolean canPlaceItem(int slotIndex, ItemStack itemStack) {
		return !SlotUtil.isSlotInRange(slotIndex, SLOT_FRAMES_1, SLOT_FRAMES_COUNT) &&
			super.canPlaceItem(slotIndex, itemStack);
	}

	public Collection<Pair<IHiveFrame, ItemStack>> getFrames() {
		Collection<Pair<IHiveFrame, ItemStack>> hiveFrames = new ArrayList<>(SLOT_FRAMES_COUNT);

		for (int i = SLOT_FRAMES_1; i < SLOT_FRAMES_1 + SLOT_FRAMES_COUNT; i++) {
			ItemStack stackInSlot = getItem(i);
			Item itemInSlot = stackInSlot.getItem();
			if (itemInSlot instanceof IHiveFrame frame) {
				hiveFrames.add(new Pair<>(frame, stackInSlot.copy()));
			}
		}

		return hiveFrames;
	}

	public void wearOutFrames(IBeeHousing beeHousing, int amount) {
		//IBeekeepingMode beekeepingMode = SpeciesUtil.BEE_TYPE.get().getBeekeepingMode(beeHousing.getWorldObj());
		int wear = amount; /* Math.round(amount * beekeepingMode.getWearModifier())*/

		for (int i = SLOT_FRAMES_1; i < SLOT_FRAMES_1 + SLOT_FRAMES_COUNT; i++) {
			ItemStack hiveFrameStack = getItem(i);
			Item hiveFrameItem = hiveFrameStack.getItem();

			if (hiveFrameItem instanceof IHiveFrame hiveFrame) {
				IIndividual queen = IIndividualHandlerItem.getIndividual(getQueen());

				if (queen != null) {
					ItemStack usedFrame = hiveFrame.frameUsed(beeHousing, hiveFrameStack, (IBee) queen, wear);
					setItem(i, usedFrame);
				}
			}
		}
	}
}
