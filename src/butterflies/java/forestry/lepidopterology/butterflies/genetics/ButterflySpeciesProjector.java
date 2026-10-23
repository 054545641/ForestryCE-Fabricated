package forestry.lepidopterology.butterflies.genetics;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

import forestry.Forestry;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.IKaryotype;
import forestry.api.lepidopterology.genetics.IButterflySpeciesType;
import forestry.core.engine.genetics.SpeciesProjection;
import forestry.lepidopterology.butterflies.ButterflySpecies;

/** Projects a validated definition into the candidate domain snapshot. */
public final class ButterflySpeciesProjector {
	private ButterflySpeciesProjector() {
	}

	/** Projects a validated definition into the candidate domain snapshot. */
	public static ButterflySpecies project(IButterflySpeciesType type, Identifier id, ButterflySpeciesDefinition def) {
			IKaryotype karyotype = type.getKaryotype();
			IGenome genome = SpeciesProjection.buildGenome(karyotype, id, def);
			return new ButterflySpecies(id, type, genome, new DefinitionButterflySpeciesBuilder(def));	}
}
