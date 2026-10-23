package forestry.api.core.machines;

import forestry.api.core.fluids.FluidVolume;

public interface IHygroregulatorRecipe extends IForestryRecipe {
	/**
	 * @return FluidVolume containing information on fluid and amount.
	 */
	FluidVolume getInputFluid();

	/**
	 * @return How long the temperature change from this recipe will last before more fluid is consumed.
	 */
	int getRetainTime();

	/**
	 * @return The humidity change that this recipe causes in one work cycle.
	 */
	byte getHumiditySteps();

	/**
	 * @return The temperature change that this recipe causes in one work cycle.
	 */
	byte getTemperatureSteps();
}
