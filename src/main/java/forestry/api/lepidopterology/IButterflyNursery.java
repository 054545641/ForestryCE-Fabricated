package forestry.api.lepidopterology;

import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.ILocationProvider;
import forestry.api.core.genetics.IIndividual;
import forestry.api.lepidopterology.genetics.IButterfly;

import javax.annotation.Nullable;

/**
 * A butterfly nursery is a place, usually a leaf block, where caterpillars laid by mated butterflies mature into cocoons.
 */
public interface IButterflyNursery extends ILocationProvider, IClimateProvider {
	@Nullable
	IButterfly getCaterpillar();

	/**
	 * @return The butterfly who created this nursery.
	 */
	@Nullable
	IIndividual getNanny();

	void setCaterpillar(@Nullable IButterfly caterpillar);

	boolean canNurse(IButterfly caterpillar);
}
