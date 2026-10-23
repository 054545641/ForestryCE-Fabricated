package forestry.apiculture.features;

import forestry.api.ForestryRegistries;
import forestry.api.core.ProductType;
import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.bees.genetics.FireworkProduct;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

/**
 * Apiculture's product types. Core owns the registry and its dispatch codec; the module that owns a product
 * registers it.
 */
@FeatureProvider
public class ApicultureProductTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final RegistrationBatch<ProductType<?>> PRODUCT_TYPES = REGISTRY.getRegistry(ForestryRegistries.Keys.PRODUCT_TYPE);

	public static final RegistryEntry<ProductType<?>, ProductType<FireworkProduct>> FIREWORK = PRODUCT_TYPES.register("firework", () -> FireworkProduct.TYPE);
}
