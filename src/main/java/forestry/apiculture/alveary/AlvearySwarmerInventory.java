package forestry.apiculture.alveary;

import forestry.api.IForestryApi;
import forestry.apiculture.alveary.multiblock.AlvearySwarmerBlockEntity;
import forestry.core.platform.inventory.InventoryAdapterTile;
import net.minecraft.world.item.ItemStack;

public class AlvearySwarmerInventory extends InventoryAdapterTile<AlvearySwarmerBlockEntity> {
	public AlvearySwarmerInventory(AlvearySwarmerBlockEntity alvearySwarmer) {
		super(alvearySwarmer, 4, "SwarmInv");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return IForestryApi.INSTANCE.getHiveManager().getSwarmingMaterialChance(stack.getItem()) != 0f;
	}
}
