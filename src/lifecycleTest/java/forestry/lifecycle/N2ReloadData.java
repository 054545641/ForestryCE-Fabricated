package forestry.lifecycle;

import forestry.api.ForestryDataMaps;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.core.machines.IStillRecipe;
import forestry.api.core.machines.fuels.*;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static forestry.lifecycle.PostalTests.check;

/** Additional production definition, machine recipe and all ten DataMap paths. */
final class N2ReloadData {
	private static final Identifier RECIPE = Identifier.parse("forestrylifecycletest:reload_still");
	static void write(Path root) throws Exception {
		write(root, "data/forestry/tree_species/birch.json", """
			{"escritoire_color":8431445,"genome":{"forestry:maturation":{"value":6}},"genus":"betula","species":"pendula"}
			""");
		write(root, "data/forestry/butterfly_species/brimstone.json", """
			{"dominant":true,"genus":"gonepteryx","rarity":0.314,"serum_color":15789624,"species":"rhamni"}
			""");
		write(root, "data/forestrylifecycletest/recipe/reload_still.json", """
			{"type":"forestry:still","id":"forestrylifecycletest:reload_still","time":37,"input":{"id":"minecraft:water","amount":71},"output":{"id":"minecraft:lava","amount":23}}
			""");
		for (var entry : Map.of(
			"fermenter_fuels", "{\"ferment_per_cycle\":43,\"burn_duration\":17}",
			"moistener_fuels", "{\"product\":{\"id\":\"minecraft:diamond\",\"count\":2},\"stage\":2,\"moistener_value\":83}",
			"rainmaker_fuels", "{\"duration\":1200,\"speed\":0.75,\"reverse\":false}",
			"peat_fuels", "{\"power_per_cycle\":47,\"burn_duration\":19}",
			"farm_fertilizers", "321", "swarmer_feed", "0.125").entrySet()) {
			write(root, "data/forestry/data_maps/item/" + entry.getKey() + ".json", "{\"values\":{\"minecraft:barrier\":" + entry.getValue() + "}}");
		}
		for (String name : new String[]{"biogas_fuels", "combustion_fuels", "combustion_coolants"}) {
			write(root, "data/forestry/data_maps/fluid/" + name + ".json", """
				{"values":{"minecraft:flowing_water":{"power_per_cycle":11,"burn_duration":29,"dissipation_multiplier":7}}}
				""");
		}
	}

	static void checkData(boolean changed) {
		check(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.BIRCH).getDefaultGenome().getActiveValue(TreeChromosomes.MATURATION) == (changed ? 6 : 2), "Tree definition reload/sync");
		check(Math.abs(SpeciesUtil.BUTTERFLY_TYPE.get().getSpecies(Identifier.parse("forestry:brimstone")).getRarity() - (changed ? 0.314f : 1.0f)) < 0.00001f, "Butterfly definition reload/sync");
		var recipe = RecipeUtils.getRecipe(RECIPE);
		check((recipe != null) == changed, "Added/removed machine recipe follows server");
		if (changed) {
			var still = (IStillRecipe) recipe.value();
			check(still.getCyclesPerUnit() == 37 && still.getInput().getAmount() == 71 && still.getOutput().getAmount() == 23, "Machine recipe payload round trip");
		}
		var item = Items.BARRIER.builtInRegistryHolder();
		check(java.util.Objects.equals(item.getData(ForestryDataMaps.FERMENTER_FUELS), changed ? new FermenterFuel(43, 17) : null), "Fermenter DataMap");
		check(java.util.Objects.equals(item.getData(ForestryDataMaps.RAINMAKER_FUELS), changed ? new RainmakerSubstrate(1200, 0.75f, false) : null), "Rainmaker DataMap");
		check(java.util.Objects.equals(item.getData(ForestryDataMaps.PEAT_FUELS), changed ? new PeatEngineFuel(47, 19) : null), "Peat DataMap");
		check(java.util.Objects.equals(item.getData(ForestryDataMaps.FARM_FERTILIZERS), changed ? 321 : null), "Fertilizer DataMap");
		check(java.util.Objects.equals(item.getData(ForestryDataMaps.SWARMER_FEED), changed ? 0.125f : null), "Swarmer DataMap");
		var moistener = item.getData(ForestryDataMaps.MOISTENER_FUELS);
		check(changed ? moistener != null && moistener.product().is(Items.DIAMOND) && moistener.product().getCount() == 2 && moistener.stage() == 2 && moistener.moistenerValue() == 83 : moistener == null, "Moistener template DataMap");
		for (var type : java.util.List.of(ForestryDataMaps.BIOGAS_FUELS, ForestryDataMaps.COMBUSTION_FUELS, ForestryDataMaps.COMBUSTION_COOLANTS)) {
			check(java.util.Objects.equals(Fluids.FLOWING_WATER.builtInRegistryHolder().getData(type), changed ? new BiogasEngineFuel(11, 29, 7) : null), "Fluid DataMap: " + type.id());
		}
	}

	private static void write(Path root, String relative, String json) throws Exception {
		Path path = root.resolve(relative);
		Files.createDirectories(path.getParent());
		Files.writeString(path, json);
	}
}
