package forestry.api.plugin;

import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.resources.Identifier;

/**
 * Used to create a genome. Alleles are inline values ({@link Allele}); reference chromosomes accept a
 * {@link Identifier} which is resolved (with the value's default dominance) at apply time.
 */
public interface IGenomeBuilder {
	/**
	 * Shortcut for setting a dominant boolean allele.
	 */
	default void set(IChromosome<Boolean> chromosome, boolean value) {
		set(chromosome, Allele.of(value, true));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	default void setUnchecked(IChromosome chromosome, AllelePair allele) {
		setActive(chromosome, allele.active());
		setInactive(chromosome, allele.inactive());
	}

	/**
	 * Sets both the active and inactive allele for the chromosome.
	 */
	<V> void set(IChromosome<V> chromosome, Allele<V> allele);

	/**
	 * Sets a reference chromosome (species, flower type, effect, ...) by the referenced value's ID. The value's
	 * declared dominance is resolved at apply time.
	 */
	void set(IChromosome<Identifier> chromosome, Identifier id);

	/**
	 * Sets the active (expressed) allele for the chromosome.
	 */
	<V> void setActive(IChromosome<V> chromosome, Allele<V> allele);

	/**
	 * Sets the inactive (non-expressed) allele for the chromosome.
	 */
	<V> void setInactive(IChromosome<V> chromosome, Allele<V> allele);

	/**
	 * @return A new genome. Later modifications to this builder will not affect the returned genome.
	 */
	IGenome build();

	/**
	 * @return {@code true} if no chromosomes were set in this genome.
	 */
	boolean isEmpty();

	/**
	 * Sets the remaining unset chromosomes to their karyotype defaults.
	 */
	void setRemainingDefault();
}
