package forestry.core.platform.client.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** The old ignition/smoke objects were emitters: visible billboards are supplied by vanilla smoke particles. */
public class SmokeEmitterParticle extends NoRenderParticle {
	private final SimpleParticleType smoke;
	protected SmokeEmitterParticle(ClientLevel level, double x, double y, double z, float rise, SimpleParticleType smoke) {
		super(level, x, y, z, 0, 0, 0);
		this.smoke = smoke;
		this.xd *= 0.8;
		this.zd *= 0.8;
		this.yd = this.random.nextFloat() * rise + 0.05f;
		this.lifetime = (int) (16 / (this.random.nextDouble() * 0.8 + 0.2));
	}
	protected void emitSmoke(SimpleParticleType type) { this.level.addParticle(type, this.x, this.y, this.z, this.xd, this.yd, this.zd); }
	@Override public void tick() {
		this.xo = this.x; this.yo = this.y; this.zo = this.z;
		if (this.age++ >= this.lifetime) { remove(); return; }
		if (this.random.nextFloat() > (float) this.age / this.lifetime * 2) {
			emitSmoke(this.smoke);
		}
		this.yd -= 0.03;
		move(this.xd, this.yd, this.zd);
		this.xd *= 0.999; this.yd *= 0.999; this.zd *= 0.999;
		if (this.onGround) { this.xd *= 0.7; this.zd *= 0.7; }
	}
}
