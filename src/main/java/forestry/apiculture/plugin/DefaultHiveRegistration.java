package forestry.apiculture.plugin;
import forestry.api.apiculture.*;
import forestry.api.core.genetics.alleles.*;
import forestry.api.plugin.IApicultureRegistration;
import forestry.apiculture.hives.HiveDefinition;
import forestry.apiculture.bees.EnumHoneyComb;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Supplier;
final class DefaultHiveRegistration {
 static void register(IApicultureRegistration apiculture) {
		// Default hives
		Supplier<List<ItemStack>> honeyComb = getHoneyComb(EnumHoneyComb.HONEY);
		Supplier<List<ItemStack>> parchedComb = getHoneyComb(EnumHoneyComb.PARCHED);
		Supplier<List<ItemStack>> silkyComb = getHoneyComb(EnumHoneyComb.SILKY);
		Supplier<List<ItemStack>> mysteriousComb = getHoneyComb(EnumHoneyComb.MYSTERIOUS);
		Supplier<List<ItemStack>> frozenComb = getHoneyComb(EnumHoneyComb.FROZEN);
		Supplier<List<ItemStack>> mossyComb = getHoneyComb(EnumHoneyComb.MOSSY);
		Supplier<List<ItemStack>> spongeComb = getHoneyComb(EnumHoneyComb.SPONGY);
		Supplier<List<ItemStack>> simmerComb = getHoneyComb(EnumHoneyComb.SIMMERING);

		apiculture.registerHive(ForestryBeeSpecies.FOREST, HiveDefinition.FOREST)
			.setGenerationChance(HiveDefinition.FOREST.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.FOREST, honeyComb, 0.7f)
			.addDrop(0.08, ForestryBeeSpecies.FOREST, honeyComb, 0.0f, Map.of(BeeChromosomes.TOLERATES_RAIN, ForestryAlleles.TRUE))
			.addDrop(0.08, ForestryBeeSpecies.VALIANT, honeyComb);

		apiculture.registerHive(ForestryBeeSpecies.MEADOWS, HiveDefinition.MEADOWS)
			.setGenerationChance(HiveDefinition.MEADOWS.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.MEADOWS, honeyComb, 0.7f)
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, honeyComb);

		apiculture.registerHive(ForestryBeeSpecies.MODEST, HiveDefinition.DESERT)
			.setGenerationChance(HiveDefinition.DESERT.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.MODEST, parchedComb, 0.7f)
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, parchedComb);

		apiculture.registerHive(ForestryBeeSpecies.TROPICAL, HiveDefinition.JUNGLE)
			.setGenerationChance(HiveDefinition.JUNGLE.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.TROPICAL, silkyComb, 0.7f)
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, silkyComb);

		apiculture.registerHive(ForestryBeeSpecies.ENDED, HiveDefinition.END)
			.setGenerationChance(HiveDefinition.END.defaultGenChance())
			.addDrop(0.90, ForestryBeeSpecies.ENDED, mysteriousComb, 0.7f)
			.addDrop(0.09, ForestryBeeSpecies.ENDED, mysteriousComb, 0.7f, Map.of(BeeChromosomes.EFFECT, Allele.reference(ForestryBeeEffects.PHASING)))
			.addDrop(0.03, ForestryBeeSpecies.ENDED, mysteriousComb, 0.7f, Map.of(BeeChromosomes.EFFECT, Allele.reference(ForestryBeeEffects.ASCENSION)));

		apiculture.registerHive(ForestryBeeSpecies.WINTRY, HiveDefinition.SNOW)
			.setGenerationChance(HiveDefinition.SNOW.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.WINTRY, frozenComb, 0.5f)
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, frozenComb);

		apiculture.registerHive(ForestryBeeSpecies.MARSHY, HiveDefinition.SWAMP)
			.setGenerationChance(HiveDefinition.SWAMP.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.MARSHY, mossyComb, 0.7f)
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, mossyComb);

		apiculture.registerHive(ForestryBeeSpecies.SAVANNA, HiveDefinition.SAVANNA)
			.setGenerationChance(HiveDefinition.SAVANNA.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.SAVANNA, parchedComb, 0.7f)
			.addDrop(0.35, ForestryBeeSpecies.SAVANNA, parchedComb, 0.7f, Map.of(BeeChromosomes.EFFECT, Allele.reference(ForestryBeeEffects.AGGRESSIVE)))
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, parchedComb);

		apiculture.registerHive(ForestryBeeSpecies.LUSH, HiveDefinition.LUSH)
			.setGenerationChance(HiveDefinition.LUSH.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.LUSH, honeyComb, 0.5F)
			.addDrop(0.08, ForestryBeeSpecies.VALIANT, honeyComb);

		apiculture.registerHive(ForestryBeeSpecies.AQUATIC, HiveDefinition.AQUATIC)
			.setGenerationChance(HiveDefinition.AQUATIC.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.AQUATIC, spongeComb, 0.4F)
			.addDrop(0.03, ForestryBeeSpecies.VALIANT, spongeComb);

		apiculture.registerHive(ForestryBeeSpecies.EMBITTERED, HiveDefinition.NETHER)
			.setGenerationChance(HiveDefinition.NETHER.defaultGenChance())
			.addDrop(0.80, ForestryBeeSpecies.EMBITTERED, simmerComb, 0.7F);

		// Common village bees
		apiculture.addVillageBee(ForestryBeeSpecies.FOREST, false);
		apiculture.addVillageBee(ForestryBeeSpecies.MEADOWS, false);
		apiculture.addVillageBee(ForestryBeeSpecies.MODEST, false);
		apiculture.addVillageBee(ForestryBeeSpecies.MARSHY, false);
		apiculture.addVillageBee(ForestryBeeSpecies.WINTRY, false);
		apiculture.addVillageBee(ForestryBeeSpecies.TROPICAL, false);
		apiculture.addVillageBee(ForestryBeeSpecies.SAVANNA, false);

		// Rare village bees
		apiculture.addVillageBee(ForestryBeeSpecies.FOREST, true, Map.of(BeeChromosomes.TOLERATES_RAIN, ForestryAlleles.TRUE));
		apiculture.addVillageBee(ForestryBeeSpecies.COMMON, true, Map.of(
			BeeChromosomes.TEMPERATURE_TOLERANCE, ForestryAlleles.TOLERANCE_BOTH_1,
			BeeChromosomes.HUMIDITY_TOLERANCE, ForestryAlleles.TOLERANCE_BOTH_1
		));
		apiculture.addVillageBee(ForestryBeeSpecies.VALIANT, true);

 }
 private static Supplier<List<ItemStack>> getHoneyComb(EnumHoneyComb type) {
  return () -> List.of(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry(type.getSerializedName() + "_comb"))));
 }
}
