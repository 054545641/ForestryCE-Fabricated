package forestry.core.features;

import forestry.api.ForestryRegistries;
import forestry.api.apiculture.FlowerTypeType;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.engine.genetics.flowers.PhotosynthesisFlowerType;
import forestry.core.engine.genetics.flowers.TagFlowerType;
import forestry.core.engine.genetics.flowers.WaterTagFlowerType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

/**
 * The built-in flower type serializers, dispatched by {@code IFlowerType#CODEC} in datapack flower type
 * definitions. Flower types are shared by bees and butterflies, so base registers them rather than apiculture.
 * A butterfly can then resolve its flower chromosome with no apiculture jar present.
 */
@FeatureProvider
public class CoreFlowerTypeSerializers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final RegistrationBatch<FlowerTypeType<?>> FLOWER_TYPE_SERIALIZERS = REGISTRY.getRegistry(ForestryRegistries.Keys.FLOWER_TYPE_SERIALIZER);

	public static final RegistryEntry<FlowerTypeType<?>, FlowerTypeType<TagFlowerType>> TAG = FLOWER_TYPE_SERIALIZERS.register("tag_flower_type", () -> TagFlowerType.TYPE);
	public static final RegistryEntry<FlowerTypeType<?>, FlowerTypeType<WaterTagFlowerType>> WATER_TAG = FLOWER_TYPE_SERIALIZERS.register("water_tag_flower_type", () -> WaterTagFlowerType.TYPE);
	public static final RegistryEntry<FlowerTypeType<?>, FlowerTypeType<PhotosynthesisFlowerType>> PHOTOSYNTHESIS = FLOWER_TYPE_SERIALIZERS.register("photosynthesis_flower_type", () -> PhotosynthesisFlowerType.TYPE);
}
