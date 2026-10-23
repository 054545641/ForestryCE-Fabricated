package forestry.lepidopterology.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.lepidopterology.entities.EntityButterfly;
import forestry.core.platform.registration.FeatureEntityType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

@FeatureProvider
public class LepidopterologyEntities {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.LEPIDOPTEROLOGY);

	public static final FeatureEntityType<EntityButterfly> BUTTERFLY = REGISTRY.entity(EntityButterfly::new, MobCategory.CREATURE, "butterfly", builder -> builder.sized(0.5f, 0.25f), Mob::createMobAttributes);
}
