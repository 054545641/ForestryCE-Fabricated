package forestry.api.core.genetics.alyzer;

import forestry.api.core.tooltips.ToolTip;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;

public interface IGeneticTooltipProvider<I extends IIndividual> {
	/**
	 * Adds the handled allele to the tooltip of the individual.
	 *
	 * @param toolTip The instance of the tooltip helper class.
	 * @param genome  The genome of the individual
	 */
	void addTooltip(ToolTip toolTip, IGenome genome, I individual);
}
