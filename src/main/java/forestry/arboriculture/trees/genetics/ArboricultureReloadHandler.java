package forestry.arboriculture.trees.genetics;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.Identifier;

import forestry.Forestry;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.arboriculture.trees.TreeSpecies;
import forestry.core.engine.genetics.SpeciesType;
import forestry.core.platform.util.SpeciesUtil;

public final class ArboricultureReloadHandler {
	@SuppressWarnings("unchecked")
	public static void rebuildTreeSpecies(Map<Identifier, TreeSpeciesDefinition> defs) {
		ITreeSpeciesType type = SpeciesUtil.TREE_TYPE.get();
		ImmutableMap.Builder<Identifier, ITreeSpecies> builder = ImmutableMap.builderWithExpectedSize(defs.size());
		for (Map.Entry<Identifier, TreeSpeciesDefinition> entry : defs.entrySet()) {
			Identifier id = entry.getKey();
			TreeSpecies species = TreeSpeciesProjector.project(type, id, entry.getValue());
			if (species == null) throw new IllegalArgumentException("Invalid species definition: " + id);
            builder.put(id, species);
		}
		ImmutableMap<Identifier, ITreeSpecies> allSpecies = builder.build();
		((SpeciesType<ITreeSpecies, ?>) type).setSpecies(allSpecies);
		if (allSpecies.isEmpty()) {
			Forestry.LOGGER.warn("No tree species loaded from datapacks; Forestry leaves have no species to use");
		} else {
			Forestry.LOGGER.info("Loaded {} tree species", allSpecies.size());
		}
	}

	private ArboricultureReloadHandler() {
	}
}
