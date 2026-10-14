package forestry.apiculture.bees.genetics.effects;

import forestry.core.platform.util.EntityUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Server-side entity changes shared by the resurrection and fungification effects. */
public final class BeeEffectEntities {
	private BeeEffectEntities() {
	}

	public static boolean resurrect(ItemEntity source, Item item, EntityType<?> type) {
		if (!(source.level() instanceof ServerLevel level) || !source.isAlive()
			|| !ItemStack.isSameItemSameComponents(source.getItem(), new ItemStack(item))) {
			return false;
		}
		// Datapacks may name a non-mob entity type. Reject it without consuming the drop.
		Entity created = type.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (!(created instanceof Mob mob)) {
			return false;
		}
		if (mob instanceof EnderDragon dragon) {
			dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
		}
		if (EntityUtil.spawnEntity(level, mob, source.getX(), source.getY(), source.getZ()) == null) {
			return false;
		}
		ItemStack remainder = source.getItem().copy();
		remainder.shrink(1);
		if (remainder.isEmpty()) {
			source.discard();
		} else {
			// Publish the changed stack through entity data so clients see the remaining count.
			source.setItem(remainder);
		}
		return true;
	}

	public static boolean convertCow(Cow cow) {
		if (!(cow.level() instanceof ServerLevel level) || !cow.isAlive()) {
			return false;
		}
		MushroomCow mooshroom = EntityTypes.MOOSHROOM.create(level, EntitySpawnReason.CONVERSION);
		if (mooshroom == null) {
			return false;
		}
		mooshroom.snapTo(cow.getX(), cow.getY(), cow.getZ(), cow.getYRot(), cow.getXRot());
		mooshroom.setHealth(cow.getHealth());
		mooshroom.setAge(cow.getAge());
		mooshroom.setCustomName(cow.getCustomName());
		mooshroom.setCustomNameVisible(cow.isCustomNameVisible());
		mooshroom.yBodyRot = cow.yBodyRot;
		if (cow.isPersistenceRequired()) {
			mooshroom.setPersistenceRequired();
		}
		// Mob.convertTo discards the source even when joining the level is cancelled.
		if (!level.addFreshEntity(mooshroom)) {
			return false;
		}
		cow.discard();
		level.sendParticles(ParticleTypes.EXPLOSION, cow.getX(), cow.getY() + cow.getBbHeight() / 2.0,
			cow.getZ(), 1, 0, 0, 0, 0);
		return true;
	}
}
