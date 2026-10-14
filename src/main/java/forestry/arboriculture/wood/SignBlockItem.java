package forestry.arboriculture.wood;

import forestry.arboriculture.features.ArboricultureBlocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;

public class SignBlockItem extends SignItem {
	public SignBlockItem(ForestryStandingSignBlock block, Item.Properties properties) {
		super(block, ArboricultureBlocks.WALL_SIGN.get(block.getWoodType()).block(), properties);
	}
}
