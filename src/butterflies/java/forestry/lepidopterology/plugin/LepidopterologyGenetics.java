package forestry.lepidopterology.plugin;

import java.util.function.Consumer;
import forestry.api.apiculture.ForestryFlowerTypes;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.lepidopterology.butterflies.genetics.ButterflySpeciesType;
import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

import forestry.api.core.Product;
import forestry.api.lepidopterology.ForestryButterflyEffects;
import forestry.api.lepidopterology.ForestryCocoons;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IGeneticRegistration;
import forestry.api.plugin.ILepidopterologyRegistration;

import forestry.core.content.resources.EnumCraftingMaterial;
import forestry.lepidopterology.butterflies.DummyButterflyEffect;
import forestry.lepidopterology.butterflies.LepidopterologyFilterRule;
import forestry.lepidopterology.butterflies.LepidopterologyFilterRuleType;
import forestry.lepidopterology.butterflies.genetics.DefaultCocoon;
import forestry.lepidopterology.plugin.DefaultButterflySpecies;

/**
 * Base Forestry's lepidopterology registrations. Split out of
 * {@code forestry.core.plugin.DefaultForestryPlugin} so the base artifact does not register butterfly
 * content.
 */
public class LepidopterologyGenetics implements IForestryPlugin {
	@Override
	public void registerGenetics(IGeneticRegistration genetics) {
		// Butterfly type
		genetics.registerSpeciesType(ForestrySpeciesTypes.BUTTERFLY, ButterflySpeciesType::new)
			.setKaryotype(karyotype -> {
				karyotype.setSpecies(ButterflyChromosomes.SPECIES, ForestryButterflySpecies.MONARCH);
				karyotype.set(ButterflyChromosomes.SIZE, ForestryAlleles.SIZE_SMALL);
				karyotype.set(ButterflyChromosomes.SPEED, ForestryAlleles.SPEED_SLOWEST);
				karyotype.set(ButterflyChromosomes.LIFESPAN, ForestryAlleles.LIFESPAN_SHORTER);
				karyotype.set(ButterflyChromosomes.METABOLISM, ForestryAlleles.METABOLISM_SLOWER);
				karyotype.set(ButterflyChromosomes.FERTILITY, ForestryAlleles.FERTILITY_3);
				karyotype.set(ButterflyChromosomes.TEMPERATURE_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.HUMIDITY_TOLERANCE, ForestryAlleles.TOLERANCE_NONE)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.NEVER_SLEEPS, false)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.TOLERATES_RAIN, false)
					.setWeaklyInherited(true);
				karyotype.set(ButterflyChromosomes.FIREPROOF, false);
				karyotype.set(ButterflyChromosomes.FLOWER_TYPE, ForestryFlowerTypes.VANILLA);
				karyotype.set(ButterflyChromosomes.EFFECT, ForestryButterflyEffects.NONE);
				karyotype.set(ButterflyChromosomes.COCOON, ForestryCocoons.DEFAULT);
			})
			.addStages(ButterflyLifeStage.BUTTERFLY, ButterflyLifeStage.SERUM, ButterflyLifeStage.CATERPILLAR, ButterflyLifeStage.COCOON)
			.setDefaultStage(ButterflyLifeStage.BUTTERFLY)
			.addResearchMaterials(map -> map.put(Items.GLASS_BOTTLE, 0.9f));

		genetics.registerFilterRuleTypes(LepidopterologyFilterRuleType.values());
		LepidopterologyFilterRule.init();
	}

	@Override
	public void registerLepidopterology(ILepidopterologyRegistration lepidopterology) {
		DefaultButterflySpecies.register(lepidopterology);

		lepidopterology.registerCocoon(ForestryCocoons.DEFAULT, new DefaultCocoon("default", List.of(
			Product.of(Items.STRING, 2, 1f),
			Product.of(Items.STRING, 1, 0.75f),
			Product.of(Items.STRING, 3, 0.25f)
		)));

		lepidopterology.registerCocoon(ForestryCocoons.SILK, new DefaultCocoon("silk", List.of(
			Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("silk_wisp")), 3, 0.75f),
			Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("silk_wisp")), 2, 0.25f)
		)));

		lepidopterology.registerEffect(ForestryButterflyEffects.NONE, new DummyButterflyEffect());
	}

	@Override
	public Identifier id() {
		return ForestryModuleIds.LEPIDOPTEROLOGY;
	}

}
