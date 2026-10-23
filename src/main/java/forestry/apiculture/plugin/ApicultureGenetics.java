package forestry.apiculture.plugin;
import net.minecraft.resources.Identifier;
import forestry.api.apiculture.ForestryActivityTypes;
import forestry.api.apiculture.ForestryBeeEffects;
import forestry.api.apiculture.ForestryBeeJubilances;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.ForestryFlowerTypes;
import forestry.api.apiculture.LightPreference;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.ForestryError;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.plugin.IApicultureRegistration;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IGeneticRegistration;
import forestry.apiculture.bees.CathemeralActivityType;
import forestry.apiculture.bees.CrepuscularActivityType;
import forestry.apiculture.bees.SingleActivityType;
import forestry.apiculture.features.ApicultureEffects;
import forestry.apiculture.bees.genetics.BeeSpeciesType;
import forestry.apiculture.bees.genetics.DefaultBeeJubilance;
import forestry.apiculture.bees.genetics.HermitBeeJubilance;
import forestry.apiculture.bees.genetics.effects.AscensionBeeEffect;
import forestry.apiculture.bees.genetics.effects.CreeperBeeEffect;
import forestry.apiculture.bees.genetics.effects.DummyBeeEffect;
import forestry.apiculture.bees.genetics.effects.ExplorationBeeEffect;
import forestry.apiculture.bees.genetics.effects.FertileBeeEffect;
import forestry.apiculture.bees.genetics.effects.FungificationBeeEffect;
import forestry.apiculture.bees.genetics.effects.GuardianBeeEffect;
import forestry.apiculture.bees.genetics.effects.IgnitionBeeEffect;
import forestry.apiculture.bees.genetics.effects.PhasingBeeEffect;
import forestry.apiculture.bees.genetics.effects.PotionBeeEffectExclusive;
import forestry.apiculture.bees.genetics.effects.RadioactiveBeeEffect;
import forestry.apiculture.bees.genetics.effects.RepulsionBeeEffect;
import forestry.apiculture.bees.genetics.effects.SculkSpreadBeeEffect;
import forestry.apiculture.bees.genetics.effects.SnowingBeeEffect;

/** Runtime genetics declarations, independent of world generation and client registration. */
public final class ApicultureGenetics implements IForestryPlugin {
public void registerGenetics(IGeneticRegistration genetics) {
        genetics.registerFilterRuleTypes(forestry.apiculture.bees.ApicultureFilterRuleType.values());
        forestry.apiculture.bees.ApicultureFilterRule.init();
		// Bee type
		genetics.registerSpeciesType(ForestrySpeciesTypes.BEE, BeeSpeciesType::new)
			.setKaryotype(karyotype -> {
				karyotype.setSpecies(BeeChromosomes.SPECIES, ForestryBeeSpecies.FOREST);
				karyotype.set(BeeChromosomes.SPEED, ForestryAlleles.SPEED_SLOWEST);
				karyotype.set(BeeChromosomes.LIFESPAN, ForestryAlleles.LIFESPAN_SHORTER);
				karyotype.set(BeeChromosomes.FERTILITY, ForestryAlleles.FERTILITY_2);
				karyotype.set(BeeChromosomes.TEMPERATURE_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.HUMIDITY_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.ACTIVITY, ForestryActivityTypes.DIURNAL)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.CAVE_DWELLING, false)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.TOLERATES_RAIN, false)
					.setWeaklyInherited(true);
				karyotype.set(BeeChromosomes.FLOWER_TYPE, ForestryFlowerTypes.VANILLA);
				karyotype.set(BeeChromosomes.TERRITORY, ForestryAlleles.TERRITORY_AVERAGE);
				karyotype.set(BeeChromosomes.EFFECT, ForestryBeeEffects.NONE);
				karyotype.set(BeeChromosomes.POLLINATION, ForestryAlleles.POLLINATION_SLOWEST);
			})
			.addStages(BeeLifeStage.DRONE, BeeLifeStage.PRINCESS, BeeLifeStage.QUEEN, BeeLifeStage.LARVAE)
			.setDefaultStage(BeeLifeStage.DRONE);
	}
public void registerApiculture(IApicultureRegistration apiculture) {
        DefaultHiveRegistration.register(apiculture);
		apiculture.registerBeeEffect(ForestryBeeEffects.NONE, new DummyBeeEffect(true));
		apiculture.registerBeeEffect(ForestryBeeEffects.RADIOACTIVE, new RadioactiveBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.CREEPER, new CreeperBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.IGNITION, new IgnitionBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.EXPLORATION, new ExplorationBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.EASTER, new DummyBeeEffect(true));
		apiculture.registerBeeEffect(ForestryBeeEffects.SNOWING, new SnowingBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.REPULSION, new RepulsionBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.FERTILE, new FertileBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.MYCOPHILIC, new FungificationBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.HAKUNA_MATATA, new PotionBeeEffectExclusive(false, ApicultureEffects.HAKUNA_MATATA.asHolder(), 20 * 60 * 3, 100, 1.0f, ApicultureEffects.MATATA.asHolder()));
		apiculture.registerBeeEffect(ForestryBeeEffects.GUARDIAN, new GuardianBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.PHASING, new PhasingBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.ASCENSION, new AscensionBeeEffect());
		apiculture.registerBeeEffect(ForestryBeeEffects.SCULK, new SculkSpreadBeeEffect());

		apiculture.registerBeeJubilance(ForestryBeeJubilances.DEFAULT, DefaultBeeJubilance.INSTANCE);
		apiculture.registerBeeJubilance(ForestryBeeJubilances.HERMIT, HermitBeeJubilance.INSTANCE);

		apiculture.registerActivityType(ForestryActivityTypes.DIURNAL, new SingleActivityType(0, 12000, ForestryError.NOT_DAY, LightPreference.ANY));
		apiculture.registerActivityType(ForestryActivityTypes.NOCTURNAL, new SingleActivityType(12000, 24000, ForestryError.NOT_NIGHT, LightPreference.DARK));
		apiculture.registerActivityType(ForestryActivityTypes.METATURNAL, new SingleActivityType(0, 24000, ForestryError.INVALID, LightPreference.ANY));
		apiculture.registerActivityType(ForestryActivityTypes.CREPUSCULAR, new CrepuscularActivityType());
		apiculture.registerActivityType(ForestryActivityTypes.CATHEMERAL, new CathemeralActivityType());
	}
 public Identifier id() { return ForestryModuleIds.APICULTURE; }
}
