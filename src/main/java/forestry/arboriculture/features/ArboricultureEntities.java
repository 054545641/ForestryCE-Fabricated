package forestry.arboriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.arboriculture.entities.ForestryBoat;
import forestry.arboriculture.entities.ForestryChestBoat;
import forestry.core.platform.registration.FeatureEntityType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.entity.MobCategory;

@FeatureProvider
public class ArboricultureEntities {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	public static final FeatureEntityType<ForestryBoat> BOAT = REGISTRY.entity(ForestryBoat::new, MobCategory.MISC, "boat", builder -> builder.sized(1.375F, 0.5625F).clientTrackingRange(10), null);
	public static final FeatureEntityType<ForestryChestBoat> CHEST_BOAT = REGISTRY.entity(ForestryChestBoat::new, MobCategory.MISC, "chest_boat", builder -> builder.sized(1.375F, 0.5625F).clientTrackingRange(10), null);
}
