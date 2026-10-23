package forestry.apiculture.features;

import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.hives.HiveDecorator;
import forestry.apiculture.worldgen.ApiaristPoolElement;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

@FeatureProvider
public class ApicultureFeatures {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final RegistrationBatch<Feature<?>> FEATURES = REGISTRY.getRegistry(Registries.FEATURE);
	public static final RegistrationBatch<StructurePoolElementType<?>> POOL_ELEMENT_TYPES = REGISTRY.getRegistry(Registries.STRUCTURE_POOL_ELEMENT);

	public static final RegistryEntry<Feature<?>, HiveDecorator> HIVE = FEATURES.register("hive", HiveDecorator::new);
	public static final RegistryEntry<StructurePoolElementType<?>, StructurePoolElementType<ApiaristPoolElement>> APIARIST_POOL_ELEMENT_TYPE = POOL_ELEMENT_TYPES.register("apiarist", () -> () -> ApiaristPoolElement.CODEC);

	public static final ResourceKey<ConfiguredFeature<?, ?>> CONFIGURED_HIVE = ResourceKey.create(Registries.CONFIGURED_FEATURE, ForestryConstants.forestry("hive"));
	public static final ResourceKey<PlacedFeature> PLACED_HIVE = ResourceKey.create(Registries.PLACED_FEATURE, ForestryConstants.forestry("hive"));
}
