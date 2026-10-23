package forestry.apiculture.client;
import forestry.apiculture.particles.ApicultureParticles;
import forestry.apiculture.particles.BeeParticleData;
import forestry.apiculture.particles.BeeTargetParticleData;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.hives.IHiveTile;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.apiculture.bees.genetics.Bee;
import forestry.apiculture.bees.genetics.effects.ThrottledBeeEffect;
import forestry.core.platform.client.ParticleRender;
import forestry.core.platform.util.VecUtil;

/**
 * Bee-specific particle spawns. Split out of {@link ParticleRender} so the base artifact does not
 * name bee types; the generic spawns and {@link ParticleRender#shouldSpawnParticle} stay in core.
 */
public class BeeParticleRender {
	public static void addBeeHiveFX(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions) {
		LevelAccessor world1 = housing.getLevel();
		ClientLevel world = (ClientLevel) world1;
		if (!ParticleRender.shouldSpawnParticle(world)) {
			return;
		}

		Vec3 particleStart = housing.getBeeFXCoordinates();

		// Avoid rendering bee particles that are too far away, they're very small.
		// At 32+ distance, have no bee particles. Make more particles up close.
		BlockPos playerPosition = Minecraft.getInstance().player.blockPosition();
		double playerDistanceSq = playerPosition.distToCenterSqr(particleStart.x, particleStart.y, particleStart.z);
		if (world.getRandom().nextInt(1024) < playerDistanceSq) {
			return;
		}

		IBeeSpecies species = genome.resolveActive(BeeChromosomes.SPECIES);
		int color = species.getOutline();

		int randomInt = world.getRandom().nextInt(100);

		if (housing instanceof IHiveTile) {
			if (((IHiveTile) housing).isAngry() || randomInt >= 85) {
				List<LivingEntity> entitiesInRange = ThrottledBeeEffect.getEntitiesInRange(genome, housing, LivingEntity.class);
				if (!entitiesInRange.isEmpty()) {
					LivingEntity entity = entitiesInRange.get(world.getRandom().nextInt(entitiesInRange.size()));
					//Particle particle = new ParticleBeeTargetEntity(world, particleStart, entity, color);
					//effectRenderer.add(particle);
					world.addParticle(new BeeTargetParticleData(entity.getId(), color), particleStart.x, particleStart.y, particleStart.z, 0, 0, 0);
					return;
				}
			}
		}

		if (randomInt < 75 && !flowerPositions.isEmpty()) {
			BlockPos destination = flowerPositions.get(world.getRandom().nextInt(flowerPositions.size()));
			//Particle particle = new ParticleBeeRoundTrip(world, particleStart, destination, color);
			//effectRenderer.add(particle);
			world.addParticle(new BeeParticleData(ApicultureParticles.BEE_ROUND_TRIP_PARTICLE.get(), destination, color), particleStart.x, particleStart.y, particleStart.z, 0, 0, 0);
		} else {
			Vec3i area = Bee.getParticleArea(genome, housing);
			Vec3i offset = housing.getBlockPos().offset(-area.getX() / 2, -area.getY() / 4, -area.getZ() / 2);
			BlockPos destination = VecUtil.getRandomPositionInArea(world.getRandom(), area).offset(offset);
			world.addParticle(new BeeParticleData(ApicultureParticles.BEE_EXPLORER_PARTICLE.get(), destination, color), particleStart.x, particleStart.y, particleStart.z, 0, 0, 0);
			//Particle particle = new ParticleBeeExplore(world, particleStart, destination, color);
			//effectRenderer.add(particle);
		}
	}

	public static void addEntitySnowFX(Level world, double x, double y, double z) {
		if (!ParticleRender.shouldSpawnParticle(world)) {
			return;
		}

		world.addParticle(ApicultureParticles.SNOW_PARTICLE.get(), x + world.getRandom().nextGaussian(), y, z + world.getRandom().nextGaussian(), 0, 0, 0);
	}

	private BeeParticleRender() {}
}
