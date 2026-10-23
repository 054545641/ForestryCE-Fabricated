package forestry.api.plugin;

import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.IKaryotype;

public interface ISpeciesTypeFactory {
	ISpeciesType<?, ?> create(IKaryotype karyotype, ISpeciesTypeBuilder builder);
}
