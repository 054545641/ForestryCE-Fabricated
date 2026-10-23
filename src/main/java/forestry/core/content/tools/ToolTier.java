package forestry.core.content.tools;

import forestry.api.ForestryTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/**
 * Forestry tool materials. 26.2: replaces the old {@code Tier} enum; tool materials are
 * records consumed by {@code Item.Properties#pickaxe/shovel/axe/sword/hoe}.
 */
public class ToolTier {
	// bronze sits between iron and stone, use iron's incorrect-for-drops tag
	public static final ToolMaterial SURVIVALIST = new ToolMaterial(
		BlockTags.INCORRECT_FOR_IRON_TOOL,
		200,
		7.0F,
		2.5F,
		10,
		ForestryTags.Items.INGOTS_BRONZE
	);
}
