package forestry.core.platform.advancements;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Fires when a player's share of the tree species they have discovered changes. Used by the
 * advancement that asks for every tree species.
 */
public class ArboricultureResearchTrigger extends SimpleCriterionTrigger<ArboricultureResearchTrigger.TriggerInstance> {
	// Deviation from 1.20.1: a trigger is a registry entry in 1.21 and no longer carries its own id.
	// See ForestryAdvancementTriggers for the name it registers under
	@Override
	public Codec<TriggerInstance> codec() {
		return TriggerInstance.CODEC;
	}

	/**
	 * Called when a tree species is discovered.
	 *
	 * @param level              The level the discovering player is in
	 * @param profile            The profile of the discovering player
	 * @param researchCompletion The share of all tree species the player has discovered
	 */
	public void trigger(Level level, GameProfile profile, double researchCompletion) {
		if (level.getServer() == null) {
			return;
		}
		ServerLevel serverLevel = level.getServer().getLevel(level.dimension());
		if (serverLevel == null) {
			return;
		}

		Player player = serverLevel.getPlayerByUUID(profile.id());
		if (player instanceof ServerPlayer serverPlayer) {
			trigger(serverPlayer, instance -> instance.check(researchCompletion));
		}
	}

	// Deviation from 1.20.1: a trigger instance is a record with a codec, not a class with a
	// serializeToJson method
	public record TriggerInstance(Optional<ContextAwarePredicate> player,
								  double percentage) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
				Codec.DOUBLE.fieldOf("percent").forGetter(TriggerInstance::percentage)
			)
			.apply(instance, TriggerInstance::new));

		/**
		 * @param percentage The share of all tree species the advancement asks for
		 * @return The criterion this trigger is added to an advancement with
		 */
		public static Criterion<TriggerInstance> checkIfResearchIsGreaterThan(double percentage) {
			return ForestryAdvancementTriggers.ARBORICULTURE_RESEARCH.createCriterion(new TriggerInstance(Optional.empty(), percentage));
		}

		/**
		 * @param amount The share of all tree species the player has discovered
		 * @return Whether the player has discovered at least the share the advancement asks for
		 */
		public boolean check(double amount) {
			return amount >= this.percentage;
		}
	}
}
