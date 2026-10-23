package forestry.api.core.climate;

import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;

/**
 * Provides climate information about an object (tile entity or multiblock)
 */
public interface IClimateProvider {
	/**
	 * The current temperature of this object represented by an enum.
	 * <p>
	 * {@link forestry.api.ForestryTags.Biomes#HELLISH_TEMPERATURE} if the biome of the object is based in the nether.
	 *
	 * @return An enum value based on the temperature of this object.
	 */
	TemperatureType temperature();

	/**
	 * @return The current humidity.
	 */
	HumidityType humidity();
}
