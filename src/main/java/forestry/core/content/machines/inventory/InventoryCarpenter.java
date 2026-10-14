package forestry.core.content.machines.inventory;

import java.util.Optional;


import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.platform.util.SlotUtil;
import forestry.core.content.machines.tiles.TileCarpenter;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class InventoryCarpenter extends InventoryAdapterTile<TileCarpenter> {
	public final static int SLOT_BOX = 9;
	public final static int SLOT_PRODUCT = 10;
	public final static int SLOT_PRODUCT_COUNT = 1;
	public final static int SLOT_CAN_INPUT = 11;
	public final static short SLOT_INVENTORY_1 = 12;
	public final static short SLOT_INVENTORY_COUNT = 18;

	public InventoryCarpenter(TileCarpenter carpenter) {
		super(carpenter, 30, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_CAN_INPUT) {
			return Optional.of(FluidUtil.getFirstStackContained(stack)).filter(fluidStack -> !fluidStack.isEmpty()).filter(f -> this.tile.getTankManager().canFillFluidType(f)).isPresent();
		} else if (slotIndex == SLOT_BOX) {
			return RecipeUtils.isCarpenterBox(RecipeUtils.getRecipeMap(this.tile.getLevel()), stack);
		} else if (canSlotAccept(SLOT_CAN_INPUT, stack) || canSlotAccept(SLOT_BOX, stack)) {
			return false;
		}

		return SlotUtil.isSlotInRange(slotIndex, SLOT_INVENTORY_1, SLOT_INVENTORY_COUNT);
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_PRODUCT;
	}
}
