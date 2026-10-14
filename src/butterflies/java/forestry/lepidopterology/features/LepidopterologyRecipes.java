package forestry.lepidopterology.features;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.CraftingBookCategory;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import forestry.api.modules.ForestryModuleIds;
import forestry.lepidopterology.recipe.ButterflyMatingRecipe;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class LepidopterologyRecipes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.LEPIDOPTEROLOGY);
	private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = REGISTRY.getRegistry(Registries.RECIPE_SERIALIZER);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ButterflyMatingRecipe>> MATING_SERIALIZER = SERIALIZERS.register("butterfly_mating", () -> new RecipeSerializer<>(
		CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.MISC).xmap(ButterflyMatingRecipe::new, ButterflyMatingRecipe::category),
		CraftingBookCategory.STREAM_CODEC.map(ButterflyMatingRecipe::new, ButterflyMatingRecipe::category).cast()));
}
