package forestry.arboriculture.plugin;

import forestry.arboriculture.worldgen.*;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.plugin.IArboricultureRegistration;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.arboriculture.wood.VanillaWoodType;
import forestry.arboriculture.leaves.ForestryLeafType;
import forestry.arboriculture.features.ArboricultureBlocks;

import static forestry.api.core.genetics.ForestryTaxa.*;
import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ForestryFruits;

public class DefaultTreeSpecies {
	public static void register(IArboricultureRegistration arboriculture) {

		//TEMPERATE LINE

		// Apple Oak (English Oak) https://www.catalogueoflife.org/data/taxon/4R5YN
		arboriculture.registerSpecies(ForestryTreeSpecies.OAK, GENUS_QUERCUS, SPECIES_OAK, false, TextColor.fromRgb(4764952), VanillaWoodType.OAK)
				.setTreeFeature(TreeFeatureVanilla::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.OAK).item()))
				.addVanillaStates(Blocks.OAK_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.OAK).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.OAK).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.OAK_SAPLING)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.APPLE);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FASTER);
				});

		// Silver Birch https://www.catalogueoflife.org/data/taxon/LPCQ
		arboriculture.registerSpecies(ForestryTreeSpecies.BIRCH, GENUS_BETULA, SPECIES_BIRCH, false, TextColor.fromRgb(8431445), VanillaWoodType.BIRCH)
				.setTreeFeature(TreeFeatureVanilla::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.BIRCH).item()))
				.addVanillaStates(Blocks.BIRCH_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.BIRCH).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.BIRCH).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.BIRCH_SAPLING)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FASTER);
				});

		// Silver Lime https://www.catalogueoflife.org/data/taxon/56WVQ
		arboriculture.registerSpecies(ForestryTreeSpecies.LIME, GENUS_TILIA, SPECIES_LIME, true, TextColor.fromRgb(0x5ea107), ForestryWoodType.LIME)
				.setTreeFeature(SilverLimeFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.LIME).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.LIME).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.LIME).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_LOWER);
				})
				.setRarity(0.005f);

		// Sour Cherry https://www.catalogueoflife.org/data/taxon/4N8QS
		// Previously known as Hill Cherry, and this introduced a world of hurt.
		arboriculture.registerSpecies(ForestryTreeSpecies.SOUR_CHERRY, GENUS_PRUNUS, SPECIES_SOUR_CHERRY, true, TextColor.fromRgb(0x84AA37), ForestryWoodType.SOUR_CHERRY)
				.setTreeFeature(SourCherryFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.SOUR_CHERRY).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.SOUR_CHERRY).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.SOUR_CHERRY).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.CHERRY);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALLER);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FAST);
				})
				.setRarity(0.0015f);

		// Common Walnut https://www.catalogueoflife.org/data/taxon/6NFN8
		arboriculture.registerSpecies(ForestryTreeSpecies.WALNUT, GENUS_JUGLANS, SPECIES_WALNUT, true, TextColor.fromRgb(0x798c55), ForestryWoodType.WALNUT)
				.setTreeFeature(WalnutFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.WALNUT).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.WALNUT).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.WALNUT).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.WALNUT);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
				});

		// Sweet Chestnut https://www.catalogueoflife.org/data/taxon/5XCVW
		arboriculture.registerSpecies(ForestryTreeSpecies.CHESTNUT, GENUS_CASTANEA, SPECIES_CHESTNUT, true, TextColor.fromRgb(0x7E8E4D), ForestryWoodType.CHESTNUT)
				.setTreeFeature(ChestnutFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.CHESTNUT).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.CHESTNUT).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.CHESTNUT).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.CHESTNUT);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
				});

		// Pear (D'Anjou) https://www.catalogueoflife.org/data/taxon/4QWMZ
		arboriculture.registerSpecies(ForestryTreeSpecies.PEAR, GENUS_PYRUS, SPECIES_PEAR, true, TextColor.fromRgb(0x448944), ForestryWoodType.PEAR)
				.setTreeFeature(PearFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.PEAR).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.PEAR).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.PEAR).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.PEAR);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_AVERAGE);
				})
				.setAuthority("Spear");

		// Plum https://www.catalogueoflife.org/data/taxon/4N8SY
		arboriculture.registerSpecies(ForestryTreeSpecies.PLUM, GENUS_PRUNUS, SPECIES_PLUM, true, TextColor.fromRgb(0x589246), ForestryWoodType.PLUM)
				.setTreeFeature(PlumFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.PLUM).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.PLUM).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.PLUM).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.PLUM);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_HIGH);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_AVERAGE);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALLER);
				})
				.setRarity(0.005f);


		// Feijoa https://www.catalogueoflife.org/data/taxon/3DXCX
		arboriculture.registerSpecies(ForestryTreeSpecies.FEIJOA, GENUS_FEIJOA, SPECIES_FEIJOA, true, TextColor.fromRgb(0x99BAA4), ForestryWoodType.FEIJOA)
				.setTreeFeature(FeijoaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.FEIJOA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.FEIJOA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.FEIJOA).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALLEST);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.FEIJOA);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_HIGHER); //This is (currently) the highest yield of all trees
				})
				.setAuthority("Spear");


		// Golden Elm https://www.catalogueoflife.org/data/taxon/7DFJZ
		arboriculture.registerSpecies(ForestryTreeSpecies.ELM, GENUS_ULMUS, SPECIES_ELM, true, TextColor.fromRgb(0xDDFA52), ForestryWoodType.ELM)
				.setTreeFeature(ElmFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.ELM).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.ELM).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.ELM).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
				})
				.setAuthority("Spear");

		// Sugar Maple https://www.catalogueoflife.org/data/taxon/94JK
		arboriculture.registerSpecies(ForestryTreeSpecies.MAPLE, GENUS_ACER, SPECIES_MAPLE, true, TextColor.fromRgb(0xd4f425), ForestryWoodType.MAPLE)
				.setTreeFeature(MapleFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.MAPLE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.MAPLE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.MAPLE).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
				})
				.setRarity(0.0025f);

		// Copper Beech https://www.catalogueoflife.org/data/taxon/3DSK5
		arboriculture.registerSpecies(ForestryTreeSpecies.BEECH, GENUS_FAGUS, SPECIES_BEECH, true, TextColor.fromRgb(0xAD301A), ForestryWoodType.BEECH)
				.setTreeFeature(BeechFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.BEECH).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.BEECH).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.BEECH).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
				})
				.setAuthority("Spear");

		// White Poplar https://www.catalogueoflife.org/data/taxon/4LVJ5
		arboriculture.registerSpecies(ForestryTreeSpecies.POPLAR, GENUS_POPULUS, SPECIES_POPLAR, true, TextColor.fromRgb(0xa3b8a5), ForestryWoodType.POPLAR)
				.setTreeFeature(PoplarFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.POPLAR).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.POPLAR).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.POPLAR).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWER);
				});

		// SWAMP LINE

		// Dark Oak (Black Oak) https://www.catalogueoflife.org/data/taxon/6X452
		// Dark Oak is the closest thing we have to a swamp tree rn. It's sorta used as a stand-in as such.
		arboriculture.registerSpecies(ForestryTreeSpecies.DARK_OAK, GENUS_QUERCUS, SPECIES_DARK_OAK, false, TextColor.fromRgb(4764952), VanillaWoodType.DARK_OAK)
				.setTreeFeature(TreeFeatureVanilla::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.DARK_OAK).item()))
				.addVanillaStates(Blocks.DARK_OAK_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.DARK_OAK).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.DARK_OAK).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.DARK_OAK_SAPLING)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FASTER);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
				})
				.setAuthority("Binnie");

		// White Willow https://www.catalogueoflife.org/data/taxon/6XCGV
		arboriculture.registerSpecies(ForestryTreeSpecies.WILLOW, GENUS_SALIX, SPECIES_WILLOW, true, TextColor.fromRgb(0xa3b8a5), ForestryWoodType.WILLOW)
				.setTreeFeature(WillowFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.WILLOW).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.WILLOW).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.WILLOW).block().getStateDefinition().getPossibleStates())
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FASTER);
				})
				.setRarity(0.0025f);

		//FLORAL LINE

		// Cherry Blossom https://www.catalogueoflife.org/data/taxon/4N97T
		arboriculture.registerSpecies(ForestryTreeSpecies.CHERRY_VANILLA, GENUS_PRUNUS, SPECIES_CHERRY_BLOSSOM, false, TextColor.fromRgb(0xf7b9dc), VanillaWoodType.CHERRY)
				.setTreeFeature(CherryVanillaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(Items.CHERRY_LEAVES))
				.addVanillaStates(Blocks.CHERRY_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.CHERRY_VANILLA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.CHERRY_VANILLA).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.CHERRY_SAPLING)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FASTER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.EFFECT, ForestryConstants.forestry("tree_effect_blossoming"));
				});

		// Flowering Dogwood https://www.catalogueoflife.org/data/taxon/YGJT
		arboriculture.registerSpecies(ForestryTreeSpecies.DOGWOOD, GENUS_CORNUS, SPECIES_DOGWOOD, true, TextColor.fromRgb(0xF4F4F4), ForestryWoodType.DOGWOOD)
				.setTreeFeature(DogwoodFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.DOGWOOD).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.DOGWOOD).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.DOGWOOD).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FAST);
				})
				.setAuthority("Spear");

		// Jacaranda https://www.catalogueoflife.org/data/taxon/99NRZ
		arboriculture.registerSpecies(ForestryTreeSpecies.JACARANDA, GENUS_JACARANDA, SPECIES_JACARANDA, true, TextColor.fromRgb(0xC18FFB), ForestryWoodType.JACARANDA)
				.setTreeFeature(JacarandaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.JACARANDA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.JACARANDA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.JACARANDA).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
				})
				.setAuthority("Spear");

		// Ipe (Yellow Ipe) https://www.catalogueoflife.org/data/taxon/99M93
		arboriculture.registerSpecies(ForestryTreeSpecies.IPE, GENUS_HANDROANTHUS, SPECIES_IPE, true, TextColor.fromRgb(0xfdd207), ForestryWoodType.IPE)
				.setTreeFeature(IpeFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.IPE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.IPE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.IPE).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
				});

		// ANCIENT LINE

		// Ginkgo https://www.catalogueoflife.org/data/taxon/3G3B3
		arboriculture.registerSpecies(ForestryTreeSpecies.GINKGO, GENUS_GINKGO, SPECIES_GINKGO, true, TextColor.fromRgb(0xFCD54A), ForestryWoodType.GINKGO)
				.setTreeFeature(GinkgoFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.GINKGO).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.GINKGO).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.GINKGO).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_HIGHER);//Highest of all trees. Best for Biofuel. Not, to my knowledge, representative of real life Ginkgo trees.
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWEST);
				})
				.setAuthority("Spear");
		//No mutations for this tree, as it comes from Sniffers.

		// CONIFEROUS LINE

		// Black Spruce https://www.catalogueoflife.org/data/taxon/4HQ3K
		arboriculture.registerSpecies(ForestryTreeSpecies.SPRUCE, GENUS_PICEA, SPECIES_SPRUCE, false, TextColor.fromRgb(6396257), VanillaWoodType.SPRUCE)
				.setTreeFeature(SpruceFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.SPRUCE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.SPRUCE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.SPRUCE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(Blocks.SPRUCE_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.SPRUCE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.SPRUCE).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.SPRUCE_SAPLING)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_AVERAGE);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FASTER);
				});

		// Mundane Larch (European Larch) https://www.catalogueoflife.org/data/taxon/6NYWF
		arboriculture.registerSpecies(ForestryTreeSpecies.LARCH, GENUS_LARIX, SPECIES_LARCH, true, TextColor.fromRgb(0x698f90), ForestryWoodType.LARCH)
				.setTreeFeature(LarchFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.LARCH).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.LARCH).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.LARCH).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.COLD)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
				})
				.setRarity(0.0025f);

		// Ponderosa Pine https://www.catalogueoflife.org/data/taxon/4J2F3
		arboriculture.registerSpecies(ForestryTreeSpecies.PINE, GENUS_PICEA, SPECIES_PINE, true, TextColor.fromRgb(0xfeff8f), ForestryWoodType.PINE)
				.setTreeFeature(PineFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.PINE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.PINE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.PINE).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.COLD)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
				})
				.setRarity(0.0025f);

		// Balsam Fir https://www.catalogueoflife.org/data/taxon/63Z6Q
		arboriculture.registerSpecies(ForestryTreeSpecies.FIR, GENUS_ABIES, SPECIES_FIR, true, TextColor.fromRgb(0x395A39), ForestryWoodType.FIR)
				.setTreeFeature(FirFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.FIR).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.FIR).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.FIR).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_HIGH);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
				})
				.setAuthority("Spear");

		// Macrocarpa (Monterey Cypress) https://www.catalogueoflife.org/data/taxon/3L5D5
		// Technically speaking it's a Monterey Cypress but I've called it Macrocarpa for two reasons:
		// 1 - No other trees have geographical places in their name. Monterey is a place in California
		// 2 - In New Zealand, we call them Macrocarpa trees, and supposedly they're known as such in other places too.
		// So yes it should give Cypress wood as well (which would be good for addons so they don't have to add it) but
		// we've also agreed that trees with mismatched names to their timbers are bad. So here we are.
		arboriculture.registerSpecies(ForestryTreeSpecies.MACROCARPA, GENUS_HESPEROCYPARIS, SPECIES_MACROCARPA, true, TextColor.fromRgb(0x5D7121), ForestryWoodType.MACROCARPA)
				.setTreeFeature(MacrocarpaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.MACROCARPA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.MACROCARPA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.MACROCARPA).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
				})
				.setAuthority("Spear");

		// Coast Sequoia (Coast Redwood) https://www.catalogueoflife.org/data/taxon/4WSQG
		arboriculture.registerSpecies(ForestryTreeSpecies.SEQUOIA, GENUS_SEQUOIA, SPECIES_SEQUOIA, true, TextColor.fromRgb(0x418e71), ForestryWoodType.SEQUOIA)
				.setTreeFeature(SequoiaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.SEQUOIA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.SEQUOIA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.SEQUOIA).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGEST);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWER);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_3);
					genome.set(TreeChromosomes.FIREPROOF, true);
				});

		// Giant Sequoia https://www.catalogueoflife.org/data/taxon/4WSQK
		arboriculture.registerSpecies(ForestryTreeSpecies.GIANT_SEQUOIA, GENUS_SEQUOIADENDRON, SPECIES_GIANT_SEQUOIA, true, TextColor.fromRgb(0x738434), ForestryWoodType.GIANT_SEQUOIA)
				.setTreeFeature(GiganteumFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.GIANT_SEQUOIA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.GIANT_SEQUOIA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.GIANT_SEQUOIA).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_GIGANTIC);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWEST);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWEST);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_4);
					genome.set(TreeChromosomes.FIREPROOF, true);
				})
				.setComplexity(10);

		// Pewen https://www.catalogueoflife.org/data/taxon/G67B
		arboriculture.registerSpecies(ForestryTreeSpecies.PEWEN, GENUS_ARAUCARIA, SPECIES_PEWEN, true, TextColor.fromRgb(0x455419), ForestryWoodType.PEWEN)
				.setTreeFeature(PewenFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.PEWEN).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.PEWEN).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.PEWEN).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					//It was a Girth 2 tree in Extra Trees, but 1x1 is more accurate to real life
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGER);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_HIGH);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWER);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
				})
				.setAuthority("Spear");

		// Kauri https://www.catalogueoflife.org/data/taxon/5TQT6
		arboriculture.registerSpecies(ForestryTreeSpecies.KAURI, GENUS_AGATHIS, SPECIES_AUSTRALIS, true, TextColor.fromRgb(0x97AF64), ForestryWoodType.KAURI)
				.setTreeFeature(KauriFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.KAURI).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.KAURI).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.KAURI).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGEST);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_HIGH);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWEST);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWEST);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_3);
				})
				.setAuthority("Spear");

		// JUNGLE LINE

		// Jungle (Cocoa Tree) https://www.catalogueoflife.org/data/taxon/56BND
		arboriculture.registerSpecies(ForestryTreeSpecies.JUNGLE, GENUS_THEOBROMA, SPECIES_CACAO, false, TextColor.fromRgb(0x4764952), VanillaWoodType.JUNGLE)
				.setTreeFeature(JungleFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.JUNGLE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.JUNGLE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.JUNGLE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(Blocks.JUNGLE_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.JUNGLE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.JUNGLE).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.JUNGLE_SAPLING)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.COCOA);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGER);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FAST);
				});

		// Teak https://www.catalogueoflife.org/data/taxon/553LY
		arboriculture.registerSpecies(ForestryTreeSpecies.TEAK, GENUS_TECTONA, SPECIES_TEAK, true, TextColor.fromRgb(0xfeff8f), ForestryWoodType.TEAK)
				.setTreeFeature(TeakFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.TEAK).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.TEAK).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.TEAK).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
				})
				.setRarity(0.0025f);

		// Kapok https://www.catalogueoflife.org/data/taxon/S2C6
		arboriculture.registerSpecies(ForestryTreeSpecies.KAPOK, GENUS_CEIBA, SPECIES_KAPOK, true, TextColor.fromRgb(0x89987b), ForestryWoodType.KAPOK)
				.setTreeFeature(KapokFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.KAPOK).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.KAPOK).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.KAPOK).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
				});

		// Balsa https://www.catalogueoflife.org/data/taxon/6SF4P
		arboriculture.registerSpecies(ForestryTreeSpecies.BALSA, GENUS_OCHROMA, SPECIES_BALSA, true, TextColor.fromRgb(0x59ac00), ForestryWoodType.BALSA)
				.setTreeFeature(BalsaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.BALSA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.BALSA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.BALSA).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_HIGH);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
				})
				.setRarity(0.0005f);

		// Sweet Orange https://en.wikipedia.org/wiki/Citrus_%C3%97_sinensis
		arboriculture.registerSpecies(ForestryTreeSpecies.ORANGE, GENUS_CITRUS, SPECIES_ORANGE, true, TextColor.fromRgb(0x57AD3F), ForestryWoodType.ORANGE)
				.setTreeFeature(OrangeFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.ORANGE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.ORANGE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.ORANGE).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALLER);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.ORANGE);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_HIGH);
				})
				.setAuthority("Spear");

		// Myrtle Ebony https://www.catalogueoflife.org/data/taxon/6CWPR
		arboriculture.registerSpecies(ForestryTreeSpecies.EBONY, GENUS_DIOSPYROS, SPECIES_EBONY, true, TextColor.fromRgb(0xa2d24a), ForestryWoodType.EBONY)
				.setTreeFeature(EbonyFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.EBONY).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.EBONY).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.EBONY).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWER);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_3);
				})
				.setRarity(0.0005f);

		// Greenheart https://www.catalogueoflife.org/data/taxon/5XW95
		arboriculture.registerSpecies(ForestryTreeSpecies.GREENHEART, GENUS_CHLOROCARDIUM, SPECIES_GREENHEART, true, TextColor.fromRgb(0x678911), ForestryWoodType.GREENHEART)
				.setTreeFeature(GreenheartFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.GREENHEART).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.GREENHEART).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.GREENHEART).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
				})
				.setRarity(0.0025f);

		// Lemon https://www.catalogueoflife.org/data/taxon/9XK4K
		arboriculture.registerSpecies(ForestryTreeSpecies.LEMON, GENUS_CITRUS, SPECIES_LEMON, true, TextColor.fromRgb(0x5C8429), ForestryWoodType.LEMON)
				.setTreeFeature(LemonFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.LEMON).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.LEMON).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.LEMON).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.LEMON);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_HIGH);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALLEST);

				});

		// Zebrano https://www.catalogueoflife.org/data/taxon/42RTY
		arboriculture.registerSpecies(ForestryTreeSpecies.ZEBRANO, GENUS_MICROBERLINIA, SPECIES_ZEBRANO, true, TextColor.fromRgb(0xa2d24a), ForestryWoodType.ZEBRANO)
				.setTreeFeature(ZebranoFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.ZEBRANO).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.ZEBRANO).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.ZEBRANO).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
				})
				.setRarity(0.0005f);

		// (Big-Leaf) Mahogany https://www.catalogueoflife.org/data/taxon/53K5Y
		arboriculture.registerSpecies(ForestryTreeSpecies.MAHOGANY, GENUS_SWIETENIA, SPECIES_MAHOGANY, true, TextColor.fromRgb(0x8ab154), ForestryWoodType.MAHOGANY)
				.setTreeFeature(MahoganyFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.MAHOGANY).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.MAHOGANY).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.MAHOGANY).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
				})
				.setRarity(0.0005f);

		// Coconut https://www.catalogueoflife.org/data/taxon/WP6H
		arboriculture.registerSpecies(ForestryTreeSpecies.COCONUT, GENUS_COCOS, SPECIES_COCONUT, true, TextColor.fromRgb(0x4F750F), ForestryWoodType.COCONUT)
				.setTreeFeature(CoconutFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.COCONUT).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.COCONUT).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.COCONUT).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_AVERAGE);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FAST);
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.COCONUT);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_HIGH);
				})
				.setAuthority("Spear");

		// Papaya https://www.catalogueoflife.org/data/taxon/RCZK
		arboriculture.registerSpecies(ForestryTreeSpecies.PAPAYA, GENUS_CARICA, SPECIES_PAPAYA, true, TextColor.fromRgb(0x74B225), ForestryWoodType.PAPAYA)
				.setTreeFeature(PapayaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.PAPAYA).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.PAPAYA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.PAPAYA).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.PAPAYA);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
				})
				.setRarity(0.005f);

		// ARID LINE

		// Acacia https://www.catalogueoflife.org/data/taxon/BSJF7
		arboriculture.registerSpecies(ForestryTreeSpecies.ACACIA_VANILLA, GENUS_ACACIA, SPECIES_ACACIA, false, TextColor.fromRgb(0x616101), VanillaWoodType.ACACIA)
				.setTreeFeature(AcaciaFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.ACACIA_VANILLA).item()))
				.addVanillaStates(Blocks.ACACIA_LEAVES.getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.ACACIA_VANILLA).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.ACACIA_VANILLA).block().getStateDefinition().getPossibleStates())
				.addVanillaSapling(Items.ACACIA_SAPLING)
				.setAuthority("Binnie");

		// Camelthorn https://www.catalogueoflife.org/data/taxon/BTCD3
		arboriculture.registerSpecies(ForestryTreeSpecies.CAMELTHORN, GENUS_VACHELLIA, SPECIES_CAMELTHORN, true, TextColor.fromRgb(0x748C1C), ForestryWoodType.CAMELTHORN)
				.setTreeFeature(CamelthornFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.CAMELTHORN).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.CAMELTHORN).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.CAMELTHORN).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.ARID)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWEST);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
				})
				.setRarity(0.005f);

		// Padauk (African Padauk) https://www.catalogueoflife.org/data/taxon/4PVKG
		arboriculture.registerSpecies(ForestryTreeSpecies.PADAUK, GENUS_PTEROCARPUS, SPECIES_PADAUK, true, TextColor.fromRgb(0xd0df8c), ForestryWoodType.PADAUK)
				.setTreeFeature(PadaukFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.PADAUK).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.PADAUK).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.PADAUK).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
				})
				.setRarity(0.005f);

		// Cocobolo https://www.catalogueoflife.org/data/taxon/33Z8J
		arboriculture.registerSpecies(ForestryTreeSpecies.COCOBOLO, GENUS_DALBERGIA, SPECIES_COCOBOLO, true, TextColor.fromRgb(0x6aa17a), ForestryWoodType.COCOBOLO)
				.setTreeFeature(CocoboloFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.COCOBOLO).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.COCOBOLO).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.COCOBOLO).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGEST);
				})
				.setRarity(0.0005f);

		// Wenge https://www.catalogueoflife.org/data/taxon/43D8S
		arboriculture.registerSpecies(ForestryTreeSpecies.WENGE, GENUS_MILLETTIA, SPECIES_WENGE, true, TextColor.fromRgb(0xada157), ForestryWoodType.WENGE)
				.setTreeFeature(WengeFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.WENGE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.WENGE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.WENGE).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWEST);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_2);
				})
				.setRarity(0.0005F);

		// Blue Mahoe  https://www.catalogueoflife.org/data/taxon/54LNR
		arboriculture.registerSpecies(ForestryTreeSpecies.MAHOE, GENUS_TALIPARITI, SPECIES_MAHOE, true, TextColor.fromRgb(0xa0ba1b), ForestryWoodType.MAHOE)
				.setTreeFeature(MahoeFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.MAHOE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.MAHOE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.MAHOE).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_HIGH);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOWEST);
				})
				.setRarity(0.000005f);

		// Grandidier's Baobab https://www.catalogueoflife.org/data/taxon/9X66
		arboriculture.registerSpecies(ForestryTreeSpecies.BAOBAB, GENUS_ADANSONIA, SPECIES_BAOBAB, true, TextColor.fromRgb(0xfeff8f), ForestryWoodType.BAOBAB)
				.setTreeFeature(BaobabFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.BAOBAB).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.BAOBAB).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.BAOBAB).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_LARGE);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWER);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_SLOW);
					genome.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_3);
				})
				.setRarity(0.005f);

		// Date Palm https://www.catalogueoflife.org/data/taxon/4GKRK
		// TODO: Should this be renamed to Date wood, or stay as Palm wood?
		arboriculture.registerSpecies(ForestryTreeSpecies.DATE, GENUS_PHOENIX, SPECIES_DATE, true, TextColor.fromRgb(0xcbcd79), ForestryWoodType.PALM)
				.setTreeFeature(DateFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.DATE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.DATE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.DATE).block().getStateDefinition().getPossibleStates())
				.setTemperature(TemperatureType.WARM)
				.setHumidity(HumidityType.DAMP)
				.setGenome(genome -> {
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.DATES);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOW);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_LOW);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_AVERAGE);
				})
				.setRarity(0.005f);

		// Olive https://www.catalogueoflife.org/data/taxon/493JT
		arboriculture.registerSpecies(ForestryTreeSpecies.OLIVE, GENUS_OLEA, SPECIES_OLIVE, true, TextColor.fromRgb(0xB7B792), ForestryWoodType.OLIVE)
				.setTreeFeature(OliveFeature::new)
				.setDecorativeLeaves(new ItemStackTemplate(ArboricultureBlocks.LEAVES_DECORATIVE.get(ForestryLeafType.OLIVE).item()))
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT.get(ForestryLeafType.OLIVE).block().getStateDefinition().getPossibleStates())
				.addVanillaStates(ArboricultureBlocks.LEAVES_DEFAULT_FRUIT.get(ForestryLeafType.OLIVE).block().getStateDefinition().getPossibleStates())
				.setGenome(genome -> {
					genome.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALLER);
					genome.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOW);
					genome.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_FAST);
					genome.set(TreeChromosomes.FRUIT, ForestryFruits.OLIVE);
					genome.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_AVERAGE);
					genome.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
				})
				.setAuthority("Spear");

	}
}
