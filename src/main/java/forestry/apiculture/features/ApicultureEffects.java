package forestry.apiculture.features;

import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

@FeatureProvider
public class ApicultureEffects {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	private static final RegistrationBatch<MobEffect> MOB_EFFECTS = REGISTRY.getRegistry(Registries.MOB_EFFECT);

	public static final RegistryEntry<MobEffect, MobEffect> HAKUNA_MATATA = MOB_EFFECTS.register("hakuna_matata", () -> {
		return new ForestryMobEffect(MobEffectCategory.BENEFICIAL, 0x069af3)
			.addAttributeModifier(Attributes.FOLLOW_RANGE, ForestryConstants.forestry("hakuna_matata_follow_range"), 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	});
	public static final RegistryEntry<MobEffect, MobEffect> MATATA = MOB_EFFECTS.register("matata", () -> {
		return new ForestryMobEffect(MobEffectCategory.NEUTRAL, 0x380835);
	});

	public static class ForestryMobEffect extends MobEffect {
		protected ForestryMobEffect(MobEffectCategory category, int color) {
			super(category, color);
		}

		// we have no ongoing effects
		@Override
		public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
			return false;
		}
	}
}
