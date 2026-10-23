package forestry.core.platform.advancements;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import forestry.core.platform.registration.RegistrationBatch;

/**
 * Forestry's own criteria. Held as instances so the code that fires one and the provider that writes
 * one into an advancement both name the same object.
 */
// Deviation from 1.20.1: CriteriaTriggers.register is gone. A trigger is an entry of the
// trigger_type registry in 1.21, so it goes in through a DeferredRegister like any other content
@FeatureProvider
public class ForestryAdvancementTriggers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final RegistrationBatch<CriterionTrigger<?>> TRIGGERS = REGISTRY.getRegistry(Registries.TRIGGER_TYPE);

	public static final DiscoverSpeciesTrigger DISCOVER_SPECIES = new DiscoverSpeciesTrigger();
	public static final ApicultureResearchTrigger APICULTURE_RESEARCH = new ApicultureResearchTrigger();
	public static final ArboricultureResearchTrigger ARBORICULTURE_RESEARCH = new ArboricultureResearchTrigger();

	static {
		TRIGGERS.register("pickup_species_trigger", () -> DISCOVER_SPECIES);
		TRIGGERS.register("apiculture_research_trigger", () -> APICULTURE_RESEARCH);
		TRIGGERS.register("arboriculture_research_trigger", () -> ARBORICULTURE_RESEARCH);
	}
}
