package forestry.apiculture.bees.genetics.effects;

import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PhasingBeeEffect extends ThrottledBeeEffect {
	public PhasingBeeEffect() {
		super(true, 40, true, true);
	}

	@Override
	IEffectData doEffectThrottled(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		ServerLevel level = (ServerLevel) housing.getLevel();
		RandomSource random = level.getRandom();
		List<LivingEntity> list = getEntitiesInRange(genome, housing, LivingEntity.class);

		for (LivingEntity entity : list) {
			int count = BeeManager.armorApiaristHelper.wearsItems(entity, this, true);

			if (count >= 4) {
				continue;
			}

			double x = entity.getX();
			double y = entity.getY();
			double z = entity.getZ();

			for (int i = 0; i < 16; i++) {
				double targetX = x + (random.nextDouble() - 0.5) * 16;
				double targetY = Mth.clamp(y + (double) (random.nextInt(16) - 8), level.getMinY(), level.getMinY() + level.getLogicalHeight() - 1);
				double targetZ = z + (random.nextDouble() - 0.5) * 16;

				if (entity.isPassenger()) {
					entity.stopRiding();
				}

				Vec3 vec3 = entity.position();
				level.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(entity));
				// TODO N1 note: the ChorusFruit teleport event no longer exists in NeoForge 26.2,
				// so this teleport is no longer cancellable by other mods
				if (entity.randomTeleport(targetX, targetY, targetZ, true)) {
					SoundEvent sound = entity instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
					level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 1f, 1f);
					entity.playSound(sound, 1f, 1f);
					break;
				}
			}
		}
		return storedData;
	}

	@Override
	public IEffectData doFX(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		Level level = housing.getLevel();
		level.addParticle(ParticleTypes.PORTAL, housing.getBlockPos().getX() + 0.5, housing.getBlockPos().getY() + 0.5 + level.getRandom().nextDouble() * 2, housing.getBlockPos().getZ() + 0.5, level.getRandom().nextGaussian(), 0, level.getRandom().nextGaussian());
		return storedData;
	}
}
