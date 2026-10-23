package forestry.core.platform.client;

import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.client.particles.ParticleIgnition;
import forestry.core.platform.client.particles.ParticleSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;


public class ParticleRender {
	private static final DustParticleOptions HONEY_DUST = new DustParticleOptions(0xE5BF00, 1.0F);

	public static boolean shouldSpawnParticle(Level world) {
		if (!world.isClientSide() || !ForestryConfig.CLIENT.showParticles.get()) {
			return false;
		}

		Minecraft mc = Minecraft.getInstance();
		ParticleStatus particleSetting = mc.options.particles().get();

		if (particleSetting == ParticleStatus.MINIMAL) { // minimal
			return world.getRandom().nextInt(10) == 0;
		} else if (particleSetting == ParticleStatus.DECREASED) { // decreased
			return world.getRandom().nextInt(3) != 0;
		} else { // all
			return true;
		}
	}


	public static void addEntityHoneyDustFX(Level world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		world.addParticle(HONEY_DUST, x, y, z, 0, 0, 0);
	}

	public static void addEntityExplodeFX(Level world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		effectRenderer.createParticle(DustParticleOptions.REDSTONE, x, y, z, 0, 0, 0);
	}


	public static void addEntityIgnitionFX(ClientLevel world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		effectRenderer.add(new ParticleIgnition(world, x, y, z));
	}

	public static void addEntitySmokeFX(Level world, double x, double y, double z) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		effectRenderer.add(new ParticleSmoke((ClientLevel) world, x, y, z));
	}

	public static void addEntityPotionFX(Level world, double x, double y, double z, int color) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		Minecraft.getInstance().particleEngine.createParticle(new DustParticleOptions(color & 0xFFFFFF, 1f), x, y, z, 0, 0, 0);
	}

	public static void addPortalFx(Level world, BlockPos pos, RandomSource rand) {
		if (!shouldSpawnParticle(world)) {
			return;
		}

		int j = rand.nextInt(2) * 2 - 1;
		int k = rand.nextInt(2) * 2 - 1;
		double xPos = (double) pos.getX() + 0.5D + 0.25D * (double) j;
		double yPos = (float) pos.getY() + rand.nextFloat();
		double zPos = (double) pos.getZ() + 0.5D + 0.25D * (double) k;
		double xSpeed = rand.nextFloat() * (float) j;
		double ySpeed = ((double) rand.nextFloat() - 0.5D) * 0.125D;
		double zSpeed = rand.nextFloat() * (float) k;
		ParticleEngine effectRenderer = Minecraft.getInstance().particleEngine;
		effectRenderer.createParticle(DustParticleOptions.REDSTONE, xPos, yPos, zPos, xSpeed, ySpeed, zSpeed);
	}
}
