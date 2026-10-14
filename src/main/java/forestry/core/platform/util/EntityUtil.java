package forestry.core.platform.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;

public abstract class EntityUtil {
	@Nullable
	public static <T extends Mob> T spawnEntity(Level world, EntityType<T> type, double x, double y, double z) {
		if (!(world instanceof ServerLevel)) {
			return null;
		}
		T entityLiving = type.create(world, EntitySpawnReason.MOB_SUMMONED);
		if (entityLiving == null) {
			return null;
		}
		return spawnEntity(world, entityLiving, x, y, z);
	}

	@Nullable
	public static <T extends Mob> T spawnEntity(Level world, T living, double x, double y, double z) {
		if (!(world instanceof ServerLevel server) || living.isRemoved()) {
			return null;
		}
		living.snapTo(x, y, z, Mth.wrapDegrees(world.getRandom().nextFloat() * 360.0f), 0.0f);
		living.yHeadRot = living.getYRot();
		living.yBodyRot = living.getYRot();
		DifficultyInstance diff = server.getCurrentDifficultyAt(BlockPos.containing(x, y, z));
		EventHooks.finalizeMobSpawn(living, server, diff, EntitySpawnReason.MOB_SUMMONED, null);
		if (!server.addFreshEntity(living)) {
			return null;
		}
		//TODO - right sound?
		living.playAmbientSound();
		return living;
	}
}
