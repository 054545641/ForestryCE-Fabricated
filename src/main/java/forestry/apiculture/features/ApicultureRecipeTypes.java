package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.api.core.machines.IHygroregulatorRecipe;
import forestry.apiculture.recipes.HygroregulatorRecipe;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

/**
 * Apiculture's recipe types. The hygroregulator is an alveary component, so its recipe type is
 * registered here rather than with the factory machines.
 */
@FeatureProvider
public class ApicultureRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureRecipeType<IHygroregulatorRecipe> HYGROREGULATOR = REGISTRY.recipeType("hygroregulator", () -> HygroregulatorRecipe.SERIALIZER);
}
