package forestry.core.content.machines.inventory;


import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.platform.util.SlotUtil;
import forestry.core.content.machines.tiles.TileMoistener;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import forestry.api.core.fluids.FluidVolume;
import forestry.core.platform.fluids.FluidHelper;

import java.util.Optional;
import forestry.api.ForestryDataMaps;

public class InventoryMoistener extends InventoryAdapterTile<TileMoistener> {
	public static final short SLOT_STASH_1 = 0;
	public static final short SLOT_STASH_COUNT = 6;
	public static final short SLOT_RESERVOIR_1 = 6;
	public static final short SLOT_RESERVOIR_COUNT = 3;
	public static final short SLOT_WORKING = 9;
	public static final short SLOT_PRODUCT = 10;
	public static final short SLOT_RESOURCE = 11;

	public InventoryMoistener(TileMoistener moistener) {
		super(moistener, 12, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_RESOURCE) {
			return RecipeUtils.getMoistenerRecipe(RecipeUtils.getRecipeMap(this.tile.getLevel()), stack) != null;
		}

		if (SlotUtil.isSlotInRange(slotIndex, SLOT_STASH_1, SLOT_STASH_COUNT)) {
			return ForestryDataMaps.MOISTENER_FUELS.get(stack.typeHolder()) != null;
		}

		if (slotIndex == SLOT_PRODUCT) {
			Optional<FluidVolume> fluidCap = Optional.of(FluidHelper.getFluidContained(stack)).filter(fluidStack -> !fluidStack.isEmpty());
			return fluidCap.map(f -> this.tile.getTankManager().canFillFluidType(f)).orElse(false);    //TODO very common pattern. Create Helper?
		}

		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		if (slotIndex == SLOT_PRODUCT) {
			return true;
		}

		if (SlotUtil.isSlotInRange(slotIndex, SLOT_STASH_1, SLOT_STASH_COUNT + SLOT_RESERVOIR_COUNT)) {
			return ForestryDataMaps.MOISTENER_FUELS.get(itemstack.typeHolder()) == null;
		}

		return false;
	}
}
