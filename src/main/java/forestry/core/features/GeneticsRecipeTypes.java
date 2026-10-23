package forestry.core.features;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.engine.genetics.mutations.MutationRecipe;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class GeneticsRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureRecipeType<MutationRecipe> BEE_MUTATION =
		REGISTRY.recipeType("bee_mutation", () -> MutationRecipe.createSerializer(ForestrySpeciesTypes.BEE));
	public static final FeatureRecipeType<MutationRecipe> TREE_MUTATION =
		REGISTRY.recipeType("tree_mutation", () -> MutationRecipe.createSerializer(ForestrySpeciesTypes.TREE));
	public static final FeatureRecipeType<MutationRecipe> BUTTERFLY_MUTATION =
		REGISTRY.recipeType("butterfly_mutation", () -> MutationRecipe.createSerializer(ForestrySpeciesTypes.BUTTERFLY));

	@Nullable
	public static FeatureRecipeType<MutationRecipe> forType(Identifier speciesTypeId) {
		if (speciesTypeId.equals(ForestrySpeciesTypes.BEE)) {
			return BEE_MUTATION;
		}
		if (speciesTypeId.equals(ForestrySpeciesTypes.TREE)) {
			return TREE_MUTATION;
		}
		if (speciesTypeId.equals(ForestrySpeciesTypes.BUTTERFLY)) {
			return BUTTERFLY_MUTATION;
		}
		return null; // unknown/third-party species type
	}
}
