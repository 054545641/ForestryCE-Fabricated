package forestry.core.platform.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;

public class ParticleSmoke extends SmokeEmitterParticle {
	public ParticleSmoke(ClientLevel level, double x, double y, double z) { super(level,x,y,z,.2f,ParticleTypes.LARGE_SMOKE); }
}
