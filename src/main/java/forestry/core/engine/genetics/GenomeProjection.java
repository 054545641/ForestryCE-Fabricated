package forestry.core.engine.genetics;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;

import forestry.Forestry;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AlleleOverride;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.IKaryotype;
import forestry.api.plugin.IGenomeBuilder;

/**
 * Species-agnostic genome-override application shared by the data-driven species projectors (bee, tree, ...):
 * applies a definition's sparse genome overrides onto a genome builder, reaching for the same three setters the
 * code-built path uses. An override naming both sides with the same allele goes through
 * {@link IGenomeBuilder#set}; one that names them separately goes through {@link IGenomeBuilder#setActive} and
 * {@link IGenomeBuilder#setInactive}, leaving an unnamed side at its karyotype or taxon default.
 * <p>
 * Reference chromosomes (non-null {@link IChromosome#resolver()}) take their dominance from the referenced value
 * rather than from the serialized allele, exactly as {@code Genome.Builder}'s {@code Identifier} overload does.
 */
public final class GenomeProjection {
	private GenomeProjection() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void applyOverrides(IGenomeBuilder builder, IKaryotype karyotype, Map<Identifier, AlleleOverride<?>> overrides) {
		for (Map.Entry<Identifier, AlleleOverride<?>> e : overrides.entrySet()) {
			IChromosome<?> chromosome = karyotype.getChromosome(e.getKey());
			if (chromosome == null) {
				throw new IllegalArgumentException("Unknown chromosome in genome override: " + e.getKey());
			}
			AlleleOverride<?> override = e.getValue();
			Allele<?> active = resolveDominance(chromosome, override.active());
			Allele<?> inactive = resolveDominance(chromosome, override.inactive());

			if (active != null && active.equals(inactive)) {
				builder.set((IChromosome) chromosome, active);
			} else {
				if (active != null) {
					builder.setActive((IChromosome) chromosome, active);
				}
				if (inactive != null) {
					builder.setInactive((IChromosome) chromosome, inactive);
				}
			}
		}
	}

	// A reference allele carries a placeholder dominance, since the real one belongs to the referenced value and is
	// only readable once registries are populated.
	@Nullable
	private static Allele<?> resolveDominance(IChromosome<?> chromosome, @Nullable Allele<?> allele) {
		IChromosome.IReferenceResolver<?> resolver = chromosome.resolver();
		if (allele == null || resolver == null) {
			return allele;
		}
		Identifier reference = (Identifier) allele.value();
		return new Allele<>(reference, resolver.isDominant(reference));
	}
}
