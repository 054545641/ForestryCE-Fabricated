package forestry.core.content.machines.recipes.jei.still;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.IStillRecipe;
import forestry.core.content.machines.blocks.BlockTypeFactoryPlain;
import forestry.core.content.machines.features.FactoryBlocks;
import forestry.core.platform.config.Constants;
import forestry.core.platform.recipes.jei.ForestryRecipeCategory;
import forestry.core.platform.recipes.jei.ForestryRecipeType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class StillRecipeCategory extends ForestryRecipeCategory<IStillRecipe> {
	private static final Identifier guiTexture = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/still.png");

	private final IDrawable tankOverlay;
	private final IDrawable icon;
	private final IDrawableAnimated progressBar;

	public StillRecipeCategory(IGuiHelper guiHelper) {
		super(guiHelper.createDrawable(guiTexture, 34, 14, 108, 60), "block.forestry.still");
		this.tankOverlay = guiHelper.createDrawable(guiTexture, 176, 0, 16, 58);

		IDrawableStatic progressBarDrawable0 = guiHelper.createDrawable(guiTexture, 176, 74, 4, 18);
		this.progressBar = guiHelper.createAnimatedDrawable(progressBarDrawable0, 20, IDrawableAnimated.StartDirection.BOTTOM, false);
		ItemStack still = new ItemStack(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.STILL).block());
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, still);
	}

	@Override
	public IRecipeType<IStillRecipe> getRecipeType() {
		return ForestryRecipeType.STILL;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, IStillRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
			.setFluidRenderer(forestry.api.core.fluids.FluidUnits.fromMilliBuckets(10000), false, 16, 58)
			.setOverlay(this.tankOverlay, 0, 0)
			.add(recipe.getInput().getFluid(), recipe.getInput().getAmount(), recipe.getInput().variant().getComponentsPatch());

		builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 1)
			.setFluidRenderer(forestry.api.core.fluids.FluidUnits.fromMilliBuckets(10000), false, 16, 58)
			.setOverlay(this.tankOverlay, 0, 0)
			.add(recipe.getOutput().getFluid(), recipe.getOutput().getAmount(), recipe.getOutput().variant().getComponentsPatch());
	}

	@Override
	public void draw(IStillRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		super.draw(recipe, recipeSlotsView, graphics, mouseX, mouseY);
		this.progressBar.draw(graphics, 50, 3);
	}
}
