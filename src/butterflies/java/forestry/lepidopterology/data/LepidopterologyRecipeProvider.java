package forestry.lepidopterology.data;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.common.Tags;

import thedarkcolour.modkit.data.MKRecipeProvider;

import forestry.core.features.CoreBlocks;
import forestry.core.features.CoreItems;
import forestry.core.platform.block.NaturalistChestBlockType;
import forestry.lepidopterology.features.LepidopterologyItems;
import forestry.lepidopterology.recipe.ButterflyMatingRecipe;

/**
 * Generates the crafting recipes for the butterflies jar. The lepidopterist's chest is a core block,
 * but its recipe names a butterfly, so it ships from here rather than from core.
 */
public class LepidopterologyRecipeProvider {
	public static void addRecipes(RecipeOutput output, MKRecipeProvider recipes, net.minecraft.core.HolderLookup.Provider registries) {
		recipes.shapedCrafting(RecipeCategory.MISC, CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.LEPIDOPTERIST_CHEST), recipe -> {
			recipe.define('#', Tags.Items.GLASS_BLOCKS_COLORLESS);
			recipe.define('X', LepidopterologyItems.BUTTERFLY_GE);
			recipe.define('Y', Tags.Items.CHESTS_WOODEN);
			recipe.pattern(" # ");
			recipe.pattern("XYX");
			recipe.pattern("XXX");
		});
		recipes.special("butterfly_mating", () -> new ButterflyMatingRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC));

		recipes.shapelessCrafting("foresters_manual_butterfly", RecipeCategory.MISC, CoreItems.FORESTERS_MANUAL, 1, Items.BOOK, LepidopterologyItems.BUTTERFLY_GE);
	}
}
