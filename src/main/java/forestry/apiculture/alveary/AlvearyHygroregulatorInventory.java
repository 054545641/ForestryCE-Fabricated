package forestry.apiculture.alveary;

import forestry.apiculture.alveary.multiblock.AlvearyHygroregulatorBlockEntity;
import forestry.core.platform.inventory.InventoryAdapterTile;
import net.minecraft.world.item.ItemStack;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.platform.fluids.FluidHelper;

import java.util.Optional;

public class AlvearyHygroregulatorInventory extends InventoryAdapterTile<AlvearyHygroregulatorBlockEntity> {
	public static final short SLOT_INPUT = 0;

	public AlvearyHygroregulatorInventory(AlvearyHygroregulatorBlockEntity alvearyHygroregulator) {
		super(alvearyHygroregulator, 1, "CanInv");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_INPUT) {
			Optional<FluidVolume> fluidCap = java.util.Optional.of(FluidHelper.getFluidContained(stack)).filter(contents -> !contents.isEmpty());
			return fluidCap.map(f -> this.tile.getTankManager().canFillFluidType(f)).orElse(false);
		}
		return false;
	}
}
