package forestry.apiculture.particles;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

@FeatureProvider
public class ApicultureParticles {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	private static final RegistrationBatch<ParticleType<?>> PARTICLE_TYPES = REGISTRY.getRegistry(Registries.PARTICLE_TYPE);

	public static final RegistryEntry<ParticleType<?>, net.minecraft.core.particles.SimpleParticleType> SNOW_PARTICLE = PARTICLE_TYPES.register("snow_particle", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(false));

	public static final RegistryEntry<ParticleType<?>, BeeParticleData.Type> BEE_EXPLORER_PARTICLE = PARTICLE_TYPES.register("bee_explore_particle", BeeParticleData.Type::new);
	public static final RegistryEntry<ParticleType<?>, BeeParticleData.Type> BEE_ROUND_TRIP_PARTICLE = PARTICLE_TYPES.register("bee_round_trip_particle", BeeParticleData.Type::new);
	public static final RegistryEntry<ParticleType<?>, BeeTargetParticleData.Type> BEE_TARGET_ENTITY_PARTICLE = PARTICLE_TYPES.register("bee_target_entity_particle", BeeTargetParticleData.Type::new);
}
