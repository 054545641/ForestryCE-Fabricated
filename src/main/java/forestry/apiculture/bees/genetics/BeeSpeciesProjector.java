package forestry.apiculture.bees.genetics;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

import forestry.Forestry;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.IKaryotype;
import forestry.apiculture.bees.BeeSpecies;
import forestry.core.engine.genetics.SpeciesProjection;

// Projection failure aborts the complete unpublished domain snapshot.
public final class BeeSpeciesProjector {
	private BeeSpeciesProjector() {
	}

	public static BeeSpecies project(IBeeSpeciesType type, Identifier id, BeeSpeciesDefinition def) {
			if (type.getJubilanceSafe(def.jubilance()) == null) {
				throw new IllegalArgumentException("Unknown jubilance for " + id + ": " + def.jubilance());
			}
			IKaryotype karyotype = type.getKaryotype();
			IGenome genome = SpeciesProjection.buildGenome(karyotype, id, def);
			return new BeeSpecies(id, type, genome, new DefinitionBeeSpeciesBuilder(def));	}
}
