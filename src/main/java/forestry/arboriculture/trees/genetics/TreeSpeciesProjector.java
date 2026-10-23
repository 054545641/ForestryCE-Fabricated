package forestry.arboriculture.trees.genetics;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

import forestry.Forestry;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.IKaryotype;
import forestry.arboriculture.trees.TreeSpecies;
import forestry.core.engine.genetics.SpeciesProjection;

/** Projects a validated definition into the candidate domain snapshot. */
public final class TreeSpeciesProjector {
	private TreeSpeciesProjector() {
	}

	public static TreeSpecies project(ITreeSpeciesType type, Identifier id, TreeSpeciesDefinition def) {
			TreeBlockBindings bindings = ((TreeSpeciesType) type).getBindings(id);
			if (bindings == null) {
				throw new IllegalArgumentException("Missing tree block bindings for " + id);
			}
			IKaryotype karyotype = type.getKaryotype();
			IGenome genome = SpeciesProjection.buildGenome(karyotype, id, def);
			return new TreeSpecies(id, type, genome, new DefinitionTreeSpeciesBuilder(def, bindings));	}
}
