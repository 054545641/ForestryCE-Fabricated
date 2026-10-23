package forestry.core.platform.particles;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import forestry.core.platform.registration.RegistrationBatch;
import forestry.core.platform.registration.RegistryEntry;

@FeatureProvider
public class CoreParticles {
	private static final RegistrationBatch<ParticleType<?>> PARTICLE_TYPES = ModFeatureRegistry.get(ForestryModuleIds.CORE).getRegistry(Registries.PARTICLE_TYPE);

	public static final RegistryEntry<ParticleType<?>, SimpleParticleType> REFRACTORY_WAX = PARTICLE_TYPES.register("refractory_wax", () -> net.fabricmc.fabric.api.particle.v1.FabricParticleTypes.simple(true));
}
