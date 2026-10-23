package forestry.core.content.energy.inventory;

import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.content.energy.tiles.CombustionEngineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.platform.fluids.FluidHelper;

import java.util.Optional;

public class InventoryEngineCombustion extends InventoryAdapterTile<CombustionEngineBlockEntity> {
	public static final short SLOT_CAN = 0;

	public InventoryEngineCombustion(CombustionEngineBlockEntity engineIron) {
		super(engineIron, 1, "Items");
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack stack, Direction side) {
		return true;
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_CAN) {
			Optional<FluidVolume> fluid = java.util.Optional.of(FluidHelper.getFluidContained(stack)).filter(fluidStack -> !fluidStack.isEmpty());
			return fluid.map(f -> this.tile.getTankManager().canFillFluidType(f)).orElse(false);
		}

		return false;
	}
}
