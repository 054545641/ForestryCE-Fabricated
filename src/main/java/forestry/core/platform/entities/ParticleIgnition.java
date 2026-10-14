package forestry.core.platform.entities;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;

public class ParticleIgnition extends SmokeEmitterParticle {
	public ParticleIgnition(ClientLevel level, double x, double y, double z) { super(level,x,y,z,.4f,ParticleTypes.SMOKE); }
}
