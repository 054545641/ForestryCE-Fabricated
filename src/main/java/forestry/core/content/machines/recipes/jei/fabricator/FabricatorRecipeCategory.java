package forestry.core.content.machines.recipes.jei.fabricator;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.IFabricatorRecipe;
import forestry.api.core.machines.IFabricatorSmeltingRecipe;
import forestry.core.platform.config.Constants;
import forestry.core.platform.recipes.jei.ForestryRecipeCategory;
import forestry.core.platform.recipes.jei.ForestryRecipeType;
import forestry.core.platform.util.JeiUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.blocks.BlockTypeFactoryPlain;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.platform.registration.FeatureBlock;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.*;

public class FabricatorRecipeCategory extends ForestryRecipeCategory<IFabricatorRecipe> {
	private final static Identifier guiTexture = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/thermionic_fabricator.png");
	private final IDrawable icon;
	@Nullable
	private final ICraftingGridHelper craftingGridHelper;

	public FabricatorRecipeCategory(IGuiHelper guiHelper) {
		super(guiHelper.createDrawable(guiTexture, 20, 16, 136, 54), "block.forestry.thermionic_fabricator");

		FeatureBlock<BlockFactoryPlain, BlockItem> fabricatorFeatureBlock = FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.FABRICATOR);
		ItemStack fabricator = new ItemStack(fabricatorFeatureBlock.block());
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, fabricator);
		this.craftingGridHelper = guiHelper.createCraftingGridHelper();
	}

	private static Map<Fluid, List<IFabricatorSmeltingRecipe>> getSmeltingInputs() {
		Map<Fluid, List<IFabricatorSmeltingRecipe>> smeltingInputs = new HashMap<>();
		RecipeUtils.getRecipes(RecipeUtils.getRecipeMap(), FactoryRecipeTypes.FABRICATOR_SMELTING)
			.forEach(smelting -> {
				Fluid fluid = smelting.getResultFluid().getFluid();
				if (!smeltingInputs.containsKey(fluid)) {
					smeltingInputs.put(fluid, new ArrayList<>());
				}

				smeltingInputs.get(fluid).add(smelting);
			});

		return smeltingInputs;
	}

	@Override
	public IRecipeType<IFabricatorRecipe> getRecipeType() {
		return ForestryRecipeType.FABRICATOR;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, IFabricatorRecipe recipe, IFocusGroup focuses) {
		FluidStack recipeLiquid = recipe.getResultFluid();
		Fluid recipeFluid = recipeLiquid.getFluid();
		List<IFabricatorSmeltingRecipe> smeltingRecipes = getSmeltingInputs().getOrDefault(recipeFluid, List.of());
		List<ItemStack> smeltingInput = smeltingRecipes.stream()
			.flatMap(s -> JeiUtil.ingredientStacks(s.getInput()).stream())
			.toList();

		builder.addSlot(RecipeIngredientRole.INPUT, 6, 32)
			.setFluidRenderer(2000, false, 16, 16)
			.addFluidStack(recipeLiquid.getFluid(), recipeLiquid.getAmount());

		ShapedRecipe craftingGridRecipe = recipe.getCraftingGridRecipe();
		List<IRecipeSlotBuilder> craftingSlots = JeiUtil.layoutSlotGrid(builder, RecipeIngredientRole.INPUT, 3, 3, 47, 1, 18);
		JeiUtil.setCraftingItems(craftingSlots, craftingGridRecipe, this.craftingGridHelper);

		// Smelting alternatives are shown separately from the nine ghost inputs.
		builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 6, 5)
			.addItemStacks(smeltingInput);

		recipe.getPlan().ifPresent(plan -> builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 119, 1)
			.add(VanillaTypes.ITEM_STACK, plan));

		builder.addSlot(RecipeIngredientRole.OUTPUT, 119, 37)
			.addItemStack(recipe.getResultItem(RecipeUtils.getRegistryAccess()));
	}
}
