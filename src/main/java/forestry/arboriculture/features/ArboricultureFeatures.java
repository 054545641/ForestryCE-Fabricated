package forestry.arboriculture.features;

import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.worldgen.TreeDecorator;
import forestry.arboriculture.worldgen.feature.ForestryTreeFeature;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

@FeatureProvider
public class ArboricultureFeatures {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	public static final RegistrationBatch<Feature<?>> FEATURES = REGISTRY.getRegistry(Registries.FEATURE);

	public static final RegistryEntry<Feature<?>, TreeDecorator> TREE_DECORATOR = FEATURES.register("tree", TreeDecorator::new);
	public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_TREE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ForestryConstants.forestry("tree"));
	public static final ResourceKey<PlacedFeature> PLACED_TREE = ResourceKey.create(Registries.PLACED_FEATURE, ForestryConstants.forestry("tree"));

	public static final RegistryEntry<Feature<?>, ForestryTreeFeature> CUSTOM_TREE = FEATURES.register("custom_tree", ForestryTreeFeature::new);
}
