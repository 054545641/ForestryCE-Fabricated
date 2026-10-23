package forestry.mail.data;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import thedarkcolour.modkit.data.MKRecipeProvider;

import forestry.api.ForestryTags;
import forestry.api.core.circuits.ICircuit;
import forestry.core.content.resources.EnumCraftingMaterial;
import forestry.core.content.resources.EnumElectronTube;
import forestry.core.data.builder.CarpenterRecipeBuilder;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.features.CoreItems;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.registration.FeatureItem;
import forestry.mail.blocks.MailBlockType;
import forestry.mail.features.MailBlocks;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.ItemStamp;
import forestry.mail.letters.LetterItem;

import static forestry.core.data.recipe.RecipeIds.id;

/**
 * Generates the crafting and carpenter recipes for the mail jar.
 */
public class MailRecipeProvider {
	private final net.minecraft.core.HolderGetter<net.minecraft.world.item.Item> items;
	private final MKRecipeProvider recipeTools;
	private MailRecipeProvider(net.minecraft.core.HolderLookup.Provider registries, MKRecipeProvider recipes) {
		this.items = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM);
		this.recipeTools = recipes;
	}
	public static void addRecipes(RecipeOutput output, MKRecipeProvider recipes, net.minecraft.core.HolderLookup.Provider registries) {
		new MailRecipeProvider(registries, recipes).generate(output, recipes);
	}
	private net.minecraft.world.item.crafting.Ingredient ingredient(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag) { return net.minecraft.world.item.crafting.Ingredient.of(this.items.getOrThrow(tag)); }
	private net.minecraft.world.item.crafting.Ingredient ingredient(net.minecraft.world.level.ItemLike... items) { return net.minecraft.world.item.crafting.Ingredient.of(items); }
	private void generate(RecipeOutput output, MKRecipeProvider recipes) {
		recipes.shapelessCrafting(RecipeCategory.MISC, MailItems.CATALOGUE, 1, Items.BOOK, ForestryTags.Items.STAMPS);
		Ingredient sealant = CompoundIngredient.of(ingredient(ForestryTags.Items.PROPOLIS), ingredient(Tags.Items.SLIME_BALLS));
		recipes.shapelessCrafting(RecipeCategory.MISC, MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.FRESH), 1, Items.PAPER, sealant);

		recipes.shapedCrafting(RecipeCategory.MISC, MailBlocks.BASE.get(MailBlockType.MAILBOX).block(), recipe -> {
			recipe.define('#', ForestryTags.Items.INGOTS_TIN);
			recipe.define('X', Tags.Items.CHESTS_WOODEN);
			recipe.define('Y', CoreItems.STURDY_CASING);
			recipe.pattern(" # ");
			recipe.pattern("#Y#");
			recipe.pattern("XXX");
		});

		Item[] emptiedLetter = MailItems.LETTERS.getRowFeatures(LetterItem.Size.EMPTY).stream()
			.map(FeatureItem::item)
			.toArray(Item[]::new);
		recipes.shapedCrafting("paper_from_letters", RecipeCategory.MISC, Items.PAPER, recipe -> {
			recipe.define('#', ingredient(emptiedLetter));
			recipe.pattern(" # ");
			recipe.pattern(" # ");
			recipe.pattern(" # ");
		});

		recipes.shapedCrafting(RecipeCategory.MISC, MailBlocks.BASE.get(MailBlockType.TRADE_STATION).block(), recipe -> {
			recipe.define('#', CoreItems.ELECTRON_TUBES.get(EnumElectronTube.BRONZE));
			recipe.define('X', Tags.Items.CHESTS_WOODEN);
			recipe.define('Y', CoreItems.STURDY_CASING);
			recipe.define('Z', CoreItems.ELECTRON_TUBES.get(EnumElectronTube.IRON));
			recipe.define('W', DataComponentIngredient.of(true, ItemCircuitBoard.createCircuitboard(EnumCircuitBoardType.REFINED, null, new ICircuit[]{})));
			recipe.pattern("Z#Z");
			recipe.pattern("#Y#");
			recipe.pattern("XWX");
		});

		Ingredient glue = CompoundIngredient.of(
			ingredient(ForestryTags.Items.DROP_HONEY),
			ingredient(Items.SLIME_BALL)
		);

		for (EnumStampDefinition stampDefinition : EnumStampDefinition.VALUES) {
			recipes.shapedCrafting(RecipeCategory.MISC, MailItems.STAMPS.get(stampDefinition), 9, recipe -> {
				recipe.define('X', stampDefinition.getCraftingIngredient(this.items));
				recipe.define('#', Items.PAPER);
				recipe.define('Z', glue);
				recipe.pattern("XXX");
				recipe.pattern("###");
				recipe.pattern("ZZZ");
			});
		}

		addCarpenterRecipes(output);
	}

	// The carpenter is a core machine, but these eight recipes make nothing but mail items
	private void addCarpenterRecipes(RecipeOutput output) {
		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			FeatureItem<ItemStamp> item = MailItems.STAMPS.get(stamp);

			new CarpenterRecipeBuilder()
				.setLiquid(ForestryFluids.SEED_OIL.getFluid(300))

				.recipe(ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, item, 9)
					.pattern("###")
					.pattern("PPP")
					.define('#', stamp.getCraftingIngredient(this.items))
					.define('P', Items.PAPER))
				.build(output, id("carpenter", item.getName()));
		}

		new CarpenterRecipeBuilder()
			.setPackagingTime(10)
			.setLiquid(new FluidStack(Fluids.WATER, 250))

			.recipe(ShapedRecipeBuilder.shaped(this.items, RecipeCategory.MISC, MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.FRESH).item())
				.pattern("###")
				.pattern("###")
				.define('#', CoreItems.CRAFTING_MATERIALS.get(EnumCraftingMaterial.WOOD_PULP)))
			.build(output, id("carpenter", "letter_pulp"));
	}
}
