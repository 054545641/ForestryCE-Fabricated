package forestry.apiculture.bees.genetics.effects;

import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.core.genetics.IEffectData;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.apiculture.bees.genetics.Bee;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.VecUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

// An effect applied to other bee hives that shouldn't stack (ex. the Chronophage and Rejuvenation effects)
public abstract class NonStackingBeeEffect implements IBeeEffect {
	private static final java.util.Set<NonStackingBeeEffect> LIVE = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    static {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_LEVEL_TICK.register(level -> {
            java.util.List<NonStackingBeeEffect> effects;
            synchronized (LIVE) { effects = java.util.List.copyOf(LIVE); }
            effects.forEach(effect -> effect.performGlobalEffect(level));
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (LIVE) { LIVE.forEach(effect -> effect.trackedOwners.clear()); }
        });
    }
    private final HashMap<ResourceKey<Level>, HashSet<BlockPos>> trackedOwners;
	private final boolean dominant;

	public NonStackingBeeEffect(boolean dominant) {
		this.dominant = dominant;
		this.trackedOwners = new HashMap<>();

		synchronized (LIVE) { LIVE.add(this); }
	}

	@Override
	public IEffectData doEffect(IGenome genome, IEffectData storedData, IBeeHousing housing) {
		// Don't spam adding to the set
		if ((housing.getLevel().getGameTime() & 64L) == 0) {
			this.trackedOwners.computeIfAbsent(housing.getLevel().dimension(), key -> new HashSet<>()).add(housing.getBlockPos());
		}
		return IBeeEffect.super.doEffect(genome, storedData, housing);
	}

	@Override
	public boolean isDominant() {
		return this.dominant;
	}

	private void performGlobalEffect(Level level) {

		if (level.isClientSide() || level.getGameTime() % IBeekeepingLogic.DEFAULT_WORK_THROTTLE != 0) {
			return;
		}

		HashSet<BlockPos> owners = this.trackedOwners.computeIfAbsent(level.dimension(), key -> new HashSet<>());
		HashSet<BlockPos> affectedHives = new HashSet<>();

		for (Iterator<BlockPos> iterator = owners.iterator(); iterator.hasNext(); ) {
			BlockPos pos = iterator.next();
			IBeeHousing housing = TileUtil.getTile(level, pos, IBeeHousing.class);

			// Don't want to call canWork twice in one tick
			if (housing != null && !housing.getErrorLogic().hasErrors()) {
				ItemStack queenStack = housing.getBeeInventory().getQueen();
				if (IIndividualHandlerItem.getLifeStage(queenStack) == BeeLifeStage.QUEEN) {
					IIndividual queen = IIndividualHandlerItem.getIndividual(queenStack);
					IGenome genome = queen.getGenome();

					if (genome.resolveActive(BeeChromosomes.EFFECT) == this || genome.resolveInactive(BeeChromosomes.EFFECT) == this) {
						IBeeModifier modifier = IForestryApi.INSTANCE.getHiveManager().createBeeHousingModifier(housing);
						Vec3i territory = Bee.getAdjustedTerritory(genome, modifier);

						affectNearbyTiles(affectedHives, level, pos, territory);

						// Skips the iterator.remove() at the end of the loop
						continue;
					}
				}
			}

			iterator.remove();
		}
	}

	private void affectNearbyTiles(HashSet<BlockPos> affectedHives, Level level, BlockPos pos, Vec3i territory) {
		BlockPos topLeft = pos.offset(VecUtil.center(territory));
		BlockPos bottomRight = topLeft.offset(territory);

		int topLeftX = topLeft.getX();
		int topLeftZ = topLeft.getZ();

		int bottomRightX = bottomRight.getX();
		int bottomRightZ = bottomRight.getZ();

		int territoryX = territory.getX();
		int territoryY = territory.getY();
		int territoryZ = territory.getZ();

		for (int x = SectionPos.blockToSectionCoord(topLeftX); x <= SectionPos.blockToSectionCoord(bottomRightX); x++) {
			for (int z = SectionPos.blockToSectionCoord(topLeftZ); z <= SectionPos.blockToSectionCoord(bottomRightZ); z++) {
				if (level.hasChunk(x, z)) {
					for (Map.Entry<BlockPos, BlockEntity> entry : level.getChunk(x, z).getBlockEntities().entrySet()) {
						BlockPos targetPos = entry.getKey();

						if (entry.getValue() instanceof IBeeHousing housing) {
							if (targetPos.equals(pos)) {
								continue;
							}
							// don't do math if already affected
							if (affectedHives.contains(targetPos)) {
								continue;
							}

							int targetX = targetPos.getX();

							if (targetX >= topLeftX && targetX < topLeftX + territoryX) {
								int targetY = targetPos.getY();

								if (targetY >= topLeft.getY() && targetY < topLeft.getY() + territoryY) {
									int targetZ = targetPos.getZ();

									if (targetZ >= topLeftZ && targetZ < topLeftZ + territoryZ) {
										if (affectedHives.add(targetPos)) {
											doEffectForHive(level, housing);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Performs the effect on another beehive. Multiple hives that perform the same effect cannot affect the same
	 * hive twice in a single effect tick, hence the name "non-stacking" bee effect..
	 *
	 * @param level   The level where the housing is located
	 * @param housing The housing to perform the effect on. It is recommended to check
	 *                {@code IBeeHousing.getErrorLogic().hasErrors()} before performing the effect.
	 */
	protected abstract void doEffectForHive(Level level, IBeeHousing housing);
}
