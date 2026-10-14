package forestry.core.content.machines.recipes.jei.carpenter;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.core.content.machines.blocks.BlockTypeFactoryPlain;
import forestry.core.platform.config.Constants;
import forestry.core.platform.recipes.jei.ForestryRecipeCategory;
import forestry.core.platform.recipes.jei.ForestryRecipeType;
import forestry.core.platform.util.JeiUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockTypeFactoryTesr;
import forestry.core.content.machines.features.FactoryBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class CarpenterRecipeCategory extends ForestryRecipeCategory<ICarpenterRecipe> {
	private final static Identifier guiTexture = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/carpenter.png");
	private final ICraftingGridHelper craftingGridHelper;
	private final IDrawableAnimated arrow;
	private final IDrawable tankOverlay;
	private final IDrawable icon;

	public CarpenterRecipeCategory(IGuiHelper guiHelper) {
		super(guiHelper.createDrawable(guiTexture, 9, 16, 158, 61), "block.forestry.carpenter");

        this.craftingGridHelper = guiHelper.createCraftingGridHelper();
		IDrawableStatic arrowDrawable = guiHelper.createDrawable(guiTexture, 176, 59, 4, 17);
		this.arrow = guiHelper.createAnimatedDrawable(arrowDrawable, 200, IDrawableAnimated.StartDirection.BOTTOM, false);
		this.tankOverlay = guiHelper.createDrawable(guiTexture, 176, 0, 16, 58);
		ItemStack carpenter = new ItemStack(FactoryBlocks.PLAIN.get(BlockTypeFactoryPlain.CARPENTER).block());
		this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, carpenter);
	}

	@Override
	public IRecipeType<ICarpenterRecipe> getRecipeType() {
		return ForestryRecipeType.CARPENTER;
	}

	@Override
	public IDrawable getIcon() {
		return this.icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ICarpenterRecipe recipe, IFocusGroup focuses) {
		CraftingRecipe craftingGridRecipe = recipe.getCraftingGridRecipe();

		ItemStack processingIngredient = recipe.getResultItem(RecipeUtils.getRegistryAccess()).copy();
		processingIngredient.setCount(1);
		builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 71, 35)
			.addItemStack(processingIngredient);

		builder.addSlot(RecipeIngredientRole.OUTPUT, 120 - 9, 56 - 16)
			.addItemStack(recipe.getResultItem(RecipeUtils.getRegistryAccess()));

		List<IRecipeSlotBuilder> craftingSlots = JeiUtil.layoutSlotGrid(builder, RecipeIngredientRole.INPUT, 3, 3, 1, 4, 18);
		JeiUtil.setCraftingItems(craftingSlots, craftingGridRecipe, this.craftingGridHelper);

		// crate comes last to match the internal inventory layout of the carpenter
		recipe.getBox().ifPresent(box -> builder.addSlot(RecipeIngredientRole.INPUT, 74, 4).add(VanillaTypes.ITEM_STACK, box));

		IRecipeSlotBuilder tankSlot = builder.addSlot(RecipeIngredientRole.INPUT, 141, 1)
			.setFluidRenderer(10000, false, 16, 58)
			.setOverlay(this.tankOverlay, 0, 0);

		FluidStack fluidResource = recipe.getInputFluid();
		if (!fluidResource.isEmpty()) {
			tankSlot.addFluidStack(fluidResource.getFluid(), fluidResource.getAmount());
		}
	}

	@Override
	public void draw(ICarpenterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
		super.draw(recipe, recipeSlotsView, graphics, mouseX, mouseY);
		this.arrow.draw(graphics, 89, 34);
	}
}
