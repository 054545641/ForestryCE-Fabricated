package forestry.core.content.machines.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.api.core.machines.*;
import forestry.core.content.machines.recipes.*;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FactoryRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FACTORY);

	public static final FeatureRecipeType<ICarpenterRecipe> CARPENTER = REGISTRY.recipeType("carpenter", () -> CarpenterRecipe.SERIALIZER);
	public static final FeatureRecipeType<ICentrifugeRecipe> CENTRIFUGE = REGISTRY.recipeType("centrifuge", () -> CentrifugeRecipe.SERIALIZER);
	public static final FeatureRecipeType<IFabricatorRecipe> FABRICATOR = REGISTRY.recipeType("fabricator", () -> FabricatorRecipe.SERIALIZER);
	public static final FeatureRecipeType<IFabricatorSmeltingRecipe> FABRICATOR_SMELTING = REGISTRY.recipeType("fabricator_smelting", () -> FabricatorSmeltingRecipe.SERIALIZER);
	public static final FeatureRecipeType<IFermenterRecipe> FERMENTER = REGISTRY.recipeType("fermenter", () -> FermenterRecipe.SERIALIZER);
	public static final FeatureRecipeType<IMoistenerRecipe> MOISTENER = REGISTRY.recipeType("moistener", () -> MoistenerRecipe.SERIALIZER);
	public static final FeatureRecipeType<ISmelterRecipe> SMELTER = REGISTRY.recipeType("smelter", () -> SmelterRecipe.SERIALIZER);
	public static final FeatureRecipeType<ISqueezerRecipe> SQUEEZER = REGISTRY.recipeType("squeezer", () -> SqueezerRecipe.SERIALIZER);
	public static final FeatureRecipeType<ISqueezerContainerRecipe> SQUEEZER_CONTAINER = REGISTRY.recipeType("squeezer_container", () -> SqueezerContainerRecipe.SERIALIZER);
	public static final FeatureRecipeType<IStillRecipe> STILL = REGISTRY.recipeType("still", () -> StillRecipe.SERIALIZER);
}
