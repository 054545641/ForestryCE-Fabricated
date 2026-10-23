package forestry.core.platform.advancements;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Fires when a player discovers one species. Used by the advancements that ask for a specific bee or
 * a specific tree.
 */
public class DiscoverSpeciesTrigger extends SimpleCriterionTrigger<DiscoverSpeciesTrigger.TriggerInstance> {
	// Deviation from 1.20.1: a trigger is a registry entry in 1.21 and no longer carries its own id.
	// See ForestryAdvancementTriggers for the name it registers under
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	/**
	 * Called when a species is discovered.
	 *
	 * @param level     The level the discovering player is in
	 * @param profile   The profile of the discovering player
	 * @param speciesId The species that was discovered
	 */
	public void trigger(Level level, GameProfile profile, Identifier speciesId) {
		if (level.getServer() == null) {
			return;
		}
		ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
		if (serverLevel == null) {
			return;
		}

		Player player = serverLevel.getPlayerByUUID(profile.id());
		if (player instanceof ServerPlayer serverPlayer) {
			trigger(serverPlayer, instance -> instance.check(speciesId));
		}
	}

	// Deviation from 1.20.1: a trigger instance is a record with a codec, not a class with a
	// serializeToJson method. The field keeps the name "tag" that 1.20.1 wrote it under
	public record TriggerInstance(Optional<ContextAwarePredicate> player,
								  Identifier species) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Identifier.CODEC.fieldOf("tag").forGetter(TriggerInstance::species)
			)
			.apply(instance, TriggerInstance::new));

		/**
		 * @param species The species the advancement asks for
		 * @return The criterion this trigger is added to an advancement with
		 */
		public static Criterion<TriggerInstance> checkDiscovered(Identifier species) {
			return ForestryAdvancementTriggers.DISCOVER_SPECIES.createCriterion(new TriggerInstance(Optional.empty(), species));
		}

		/**
		 * @param speciesId The species that was discovered
		 * @return Whether the discovered species is the one the advancement asks for
		 */
		public boolean check(Identifier speciesId) {
			return speciesId.equals(this.species);
		}
	}
}
