package forestry.core.engine.genetics;

import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.plugin.IChromosomeBuilder;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public class ChromosomeBuilder<V> implements IChromosomeBuilder<V> {
	final IChromosome<V> chromosome;
	// Exactly one of these is set: an eager data default, or a reference default ID resolved lazily.
	@Nullable
	Allele<V> defaultAllele;
	@Nullable
	Identifier defaultReferenceId;
	boolean weaklyInherited;

	public ChromosomeBuilder(IChromosome<V> chromosome) {
		this.chromosome = chromosome;
	}

	@Override
	public IChromosomeBuilder<V> setDefault(Allele<V> allele) {
		this.defaultAllele = allele;
		this.defaultReferenceId = null;
		return this;
	}

	@Override
	public IChromosomeBuilder<V> setWeaklyInherited(boolean weaklyInherited) {
		this.weaklyInherited = weaklyInherited;
		return this;
	}
}
