package forestry.core.platform.recipes.jei;

import forestry.api.ForestryConstants;
import forestry.api.core.machines.*;
import mezz.jei.api.recipe.types.IRecipeType;
import forestry.core.content.machines.recipes.jei.rainmaker.RainmakerJeiRecipe;

public class ForestryRecipeType {
	public static final IRecipeType<ICarpenterRecipe> CARPENTER = create("carpenter", ICarpenterRecipe.class);
	public static final IRecipeType<ICentrifugeRecipe> CENTRIFUGE = create("centrifuge", ICentrifugeRecipe.class);
	public static final IRecipeType<IFabricatorRecipe> FABRICATOR = create("fabricator", IFabricatorRecipe.class);
	public static final IRecipeType<IFermenterRecipe> FERMENTER = create("fermenter", IFermenterRecipe.class);
	public static final IRecipeType<IMoistenerRecipe> MOISTENER = create("moistener", IMoistenerRecipe.class);
	public static final IRecipeType<RainmakerJeiRecipe> RAINMAKER = create("rainmaker", RainmakerJeiRecipe.class);
	public static final IRecipeType<ISmelterRecipe> SMELTER = create("smelter", ISmelterRecipe.class);
	public static final IRecipeType<ISqueezerRecipe> SQUEEZER = create("squeezer", ISqueezerRecipe.class);
	public static final IRecipeType<IStillRecipe> STILL = create("still", IStillRecipe.class);

	private static <T> IRecipeType<T> create(String uid, Class<? extends T> recipeClass) {
		return IRecipeType.create(ForestryConstants.MOD_ID, uid, recipeClass);
	}
}
