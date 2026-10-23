package forestry.api.arboriculture;

import forestry.api.core.genetics.IGenome;

// used to be used by TreekeepingMode
public interface ITreeModifier {
	/**
	 * @return Float used to modify the height.
	 */
	float getHeightModifier(IGenome genome, float currentModifier);

	/**
	 * @return Float used to modify the yield.
	 */
	float getYieldModifier(IGenome genome, float currentModifier);

	/**
	 * @return Float used to modify the sappiness.
	 */
	float getSappinessModifier(IGenome genome, float currentModifier);

	/**
	 * @return Float used to modify the maturation.
	 */
	float getMaturationModifier(IGenome genome, float currentModifier);

	/**
	 * @return Float used to modify the base mutation chance.
	 */
	float getMutationModifier(IGenome genome0, IGenome genome1, float currentModifier);
}
