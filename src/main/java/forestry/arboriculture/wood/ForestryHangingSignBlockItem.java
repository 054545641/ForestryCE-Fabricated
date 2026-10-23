package forestry.arboriculture.wood;

import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;

public class ForestryHangingSignBlockItem extends HangingSignItem {
	public ForestryHangingSignBlockItem(ForestryHangingSignBlock block, Item.Properties properties) {
		super(block, ArboricultureBlocks.WALL_HANGING_SIGN.get(block.getWoodType()).block(), properties);
	}
}
