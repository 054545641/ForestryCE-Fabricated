package forestry.apiculture.bees.genetics;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.Identifier;

import forestry.Forestry;
import forestry.api.apiculture.IBeeJubilance;
import forestry.api.apiculture.IFlowerType;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.apiculture.bees.BeeSpecies;
import forestry.core.engine.genetics.SpeciesType;
import forestry.core.platform.util.SpeciesUtil;

public final class ApicultureReloadHandler {
	@SuppressWarnings("unchecked")
	public static void rebuildSpecies(Map<Identifier, BeeSpeciesDefinition> defs) {
		IBeeSpeciesType type = SpeciesUtil.BEE_TYPE.get();
		ImmutableMap.Builder<Identifier, IBeeSpecies> builder = ImmutableMap.builderWithExpectedSize(defs.size());
		for (Map.Entry<Identifier, BeeSpeciesDefinition> entry : defs.entrySet()) {
			Identifier id = entry.getKey();
			BeeSpecies species = BeeSpeciesProjector.project(type, id, entry.getValue());
			if (species == null) throw new IllegalArgumentException("Invalid species definition: " + id);
            builder.put(id, species);
		}
		ImmutableMap<Identifier, IBeeSpecies> allSpecies = builder.build();
		((SpeciesType<IBeeSpecies, ?>) type).setSpecies(allSpecies);
		Forestry.LOGGER.info("Loaded {} bee species", allSpecies.size());
	}

	public static void rebuildBeeEffects(Map<Identifier, IBeeEffect> dataDefinitions) {
		BeeSpeciesType type = (BeeSpeciesType) SpeciesUtil.BEE_TYPE.get();
		Map<Identifier, IBeeEffect> effective = new LinkedHashMap<>(type.getCodeBeeEffects());
		// datapack effects replace code-registered effects with the same ID
		effective.putAll(dataDefinitions);
		type.setBeeEffects(ImmutableMap.copyOf(effective));
	}

	public static void rebuildJubilances(Map<Identifier, IBeeJubilance> dataDefinitions) {
		BeeSpeciesType type = (BeeSpeciesType) SpeciesUtil.BEE_TYPE.get();
		Map<Identifier, IBeeJubilance> effective = new LinkedHashMap<>(type.getCodeJubilances());
		// datapack jubilances replace code-registered jubilances with the same ID
		effective.putAll(dataDefinitions);
		type.setJubilances(ImmutableMap.copyOf(effective));
	}

	private ApicultureReloadHandler() {
	}
}
