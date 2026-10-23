package forestry.api.core.machines;

import forestry.api.core.fluids.FluidVolume;

public interface IStillRecipe extends IForestryRecipe {
	/**
	 * @return Amount of work cycles required to run through the conversion once.
	 */
	int getCyclesPerUnit();

	/**
	 * @return FluidVolume representing the input liquid.
	 */
	FluidVolume getInput();

	/**
	 * @return FluidVolume representing the output liquid.
	 */
	FluidVolume getOutput();

	boolean matches(FluidVolume input);
}
