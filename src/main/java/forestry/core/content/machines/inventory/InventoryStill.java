package forestry.core.content.machines.inventory;


import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.content.machines.tiles.TileStill;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.util.Optional;

public class InventoryStill extends InventoryAdapterTile<TileStill> {
	public static final short SLOT_PRODUCT = 0;
	public static final short SLOT_RESOURCE = 1;
	public static final short SLOT_CAN = 2;

	public InventoryStill(TileStill still) {
		super(still, 3, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_RESOURCE) {
			return FluidHelper.isFillableEmptyContainer(stack);
		} else if (slotIndex == SLOT_CAN) {
			Optional<FluidStack> fluid = Optional.of(FluidUtil.getFirstStackContained(stack)).filter(fluidStack -> !fluidStack.isEmpty());
			return fluid.map(f -> this.tile.getTankManager().canFillFluidType(f)).orElse(false);
		}
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_PRODUCT;
	}
}
