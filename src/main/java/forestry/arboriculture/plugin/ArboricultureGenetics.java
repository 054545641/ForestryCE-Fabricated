package forestry.arboriculture.plugin;

import java.util.function.Consumer;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.alleles.ForestryAlleles;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.trees.genetics.TreeSpeciesType;
import java.util.List;

import forestry.core.platform.item.FruitItemType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ForestryFruits;
import forestry.api.core.Product;
import forestry.api.modules.ForestryModuleIds;
import forestry.api.plugin.IArboricultureRegistration;
import forestry.api.plugin.IForestryPlugin;
import forestry.api.plugin.IGeneticRegistration;
import forestry.api.plugin.IPollenRegistration;
import forestry.arboriculture.trees.ArboricultureFilterRuleType;
import forestry.arboriculture.fruit.DummyFruit;
import forestry.arboriculture.fruit.PodFruit;
import forestry.arboriculture.fruit.RipeningFruit;
import forestry.arboriculture.fruit.ForestryPodType;
import forestry.arboriculture.trees.genetics.BlossomingTreeEffect;
import forestry.arboriculture.trees.genetics.DummyTreeEffect;
import forestry.arboriculture.trees.genetics.TreePollenType;


/**
 * Base Forestry's arboriculture registrations. Split out of
 * {@code forestry.core.plugin.DefaultForestryPlugin} so the base artifact does not register tree content.
 */
public class ArboricultureGenetics implements IForestryPlugin {
    public static boolean doSelfPollination = false;
	@Override
	public void registerGenetics(IGeneticRegistration genetics) {
		// Tree type
		genetics.registerSpeciesType(ForestrySpeciesTypes.TREE, TreeSpeciesType::new)
			.setKaryotype(karyotype -> {
				karyotype.setSpecies(TreeChromosomes.SPECIES, ForestryTreeSpecies.OAK);
				karyotype.set(TreeChromosomes.HEIGHT, ForestryAlleles.HEIGHT_SMALL);
				karyotype.set(TreeChromosomes.SAPLINGS, ForestryAlleles.SAPLINGS_LOWER);
				karyotype.set(TreeChromosomes.FRUIT, ForestryFruits.NONE);
				karyotype.set(TreeChromosomes.YIELD, ForestryAlleles.YIELD_LOWEST);
				karyotype.set(TreeChromosomes.SAPPINESS, ForestryAlleles.SAPPINESS_LOWEST);
				karyotype.set(TreeChromosomes.EFFECT, ForestryConstants.forestry("tree_effect_none"));
				karyotype.set(TreeChromosomes.MATURATION, ForestryAlleles.MATURATION_AVERAGE);
				karyotype.set(TreeChromosomes.GIRTH, ForestryAlleles.GIRTH_1);
				karyotype.set(TreeChromosomes.FIREPROOF, false);
			})
			.addStages(TreeLifeStage.SAPLING, TreeLifeStage.POLLEN)
			.setDefaultStage(TreeLifeStage.SAPLING);

		genetics.registerFilterRuleTypes(ArboricultureFilterRuleType.values());
	}

	@Override
	public void registerArboriculture(IArboricultureRegistration arboriculture) {
		DefaultTreeSpecies.register(arboriculture);

		Identifier pomes = ForestryConstants.forestry("block/leaves/fruits.pomes");
		Identifier nuts = ForestryConstants.forestry("block/leaves/fruits.nuts");
		Identifier berries = ForestryConstants.forestry("block/leaves/fruits.berries");
		Identifier citrus = ForestryConstants.forestry("block/leaves/fruits.citrus");
		Identifier plums = ForestryConstants.forestry("block/leaves/fruits.plums");

		arboriculture.registerFruit(ForestryFruits.NONE, new DummyFruit(false));
		arboriculture.registerFruit(ForestryFruits.APPLE, new RipeningFruit(false, 10, pomes, 0xFF1C2B, 0xe3f49c, List.of(Product.of(Items.APPLE))));
		// todo match vanilla cocoa and use fortune OR better yet, make pod fruits use actual loot tables
		arboriculture.registerFruit(ForestryFruits.COCOA, new PodFruit(false, ForestryPodType.COCOA, List.of(Product.of(Items.COCOA_BEANS))));
		arboriculture.registerFruit(ForestryFruits.CHESTNUT, new RipeningFruit(true, 6, nuts, 0x76403C, 0xc4d24a, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.CHESTNUT.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.WALNUT, new RipeningFruit(true, 8, nuts, 0xBC784E, 0xc4d24a, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.WALNUT.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.CHERRY, new RipeningFruit(true, 10, berries, 0xCC1C10, 0xc4d24a, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.CHERRY.getSerializedName())))))); //Should be a Drupe, actually
		arboriculture.registerFruit(ForestryFruits.DATES, new PodFruit(false, ForestryPodType.DATES, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.DATE.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.PAPAYA, new PodFruit(false, ForestryPodType.PAPAYA, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.PAPAYA.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.LEMON, new RipeningFruit(true, 10, citrus, 0xFFD500, 0x99ff00, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.LEMON.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.PLUM, new RipeningFruit(true, 10, plums, 0x773352, 0xeeff1a, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.PLUM.getSerializedName())))))); //Should also be a drupe

		arboriculture.registerFruit(ForestryFruits.COCONUT, new PodFruit(false, ForestryPodType.COCONUT, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.COCONUT.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.PEAR, new RipeningFruit(true, 10, pomes, 0xD8D345, 0xE3DD9C, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.PEAR.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.FEIJOA, new RipeningFruit(true, 10, berries, 0x7AB15C, 0x6A7D7B, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.FEIJOA.getSerializedName())))))); //What actually is a feijoa? I couldn't find the answer.
		arboriculture.registerFruit(ForestryFruits.ORANGE, new RipeningFruit(true, 10, citrus, 0xF4842D, 0xBCA627, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.ORANGE.getSerializedName()))))));
		arboriculture.registerFruit(ForestryFruits.OLIVE, new RipeningFruit(true, 10, berries, 0xAAC348, 0x604632, List.of(Product.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ForestryConstants.forestry(FruitItemType.OLIVE.getSerializedName())))))); //Should also be a drupe

		arboriculture.registerTreeEffect(ForestryConstants.forestry("tree_effect_none"), new DummyTreeEffect(false));
		arboriculture.registerTreeEffect(ForestryConstants.forestry("tree_effect_blossoming"), new BlossomingTreeEffect());

		DefaultWoods.register(arboriculture);

		arboriculture.registerCharcoalPitWall(Blocks.CLAY, 3);
		arboriculture.registerCharcoalPitWall(Blocks.END_STONE, 6);
		arboriculture.registerCharcoalPitWall(Blocks.END_STONE_BRICKS, 6);
		arboriculture.registerCharcoalPitWall(Blocks.DIRT, 2);
		arboriculture.registerCharcoalPitWall(Blocks.GRAVEL, 1);
		arboriculture.registerCharcoalPitWall(Blocks.NETHERRACK, 3);
	}

	@Override
	public void registerPollen(IPollenRegistration pollen) {
		pollen.registerPollenType(new TreePollenType());
	}

	@Override
	public Identifier id() {
		return ForestryModuleIds.ARBORICULTURE;
	}

}
