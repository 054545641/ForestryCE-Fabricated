package forestry.api.core.genetics.filter;

import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpeciesType;

public record FilterData(ISpeciesType<?, ?> type, IIndividual individual, ILifeStage stage) {
	public FilterData(IIndividual individual, ILifeStage stage) {
		this(individual.getType(), individual, stage);
	}
}
