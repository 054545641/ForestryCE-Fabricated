package forestry.core.content.machines.inventory;


import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.content.machines.tiles.TileBottler;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.platform.fluids.FluidHelper;

import java.util.Optional;

public class InventoryBottler extends InventoryAdapterTile<TileBottler> {
	public static final short SLOT_INPUT_FULL_CONTAINER = 0;
	public static final short SLOT_INPUT_EMPTY_CONTAINER = 1;
	public static final short SLOT_EMPTYING_PROCESSING = 2;
	public static final short SLOT_FILLING_PROCESSING = 3;
	public static final short SLOT_OUTPUT_EMPTY_CONTAINER = 4;
	public static final short SLOT_OUTPUT_FULL_CONTAINER = 5;

	public InventoryBottler(TileBottler tileBottler) {
		super(tileBottler, 6, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_INPUT_EMPTY_CONTAINER) {
			return FluidHelper.isFillableContainerWithRoom(stack);
		} else if (slotIndex == SLOT_INPUT_FULL_CONTAINER) {
			Optional<FluidVolume> fluidStack = Optional.of(FluidHelper.getFluidContained(stack)).filter(contents -> !contents.isEmpty());
			return fluidStack.map(f -> f.getFluid() != Fluids.EMPTY).orElse(false);
		}
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_OUTPUT_EMPTY_CONTAINER || slotIndex == SLOT_OUTPUT_FULL_CONTAINER;
	}
}
