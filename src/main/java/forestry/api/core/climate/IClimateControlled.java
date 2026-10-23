package forestry.api.core.climate;

/**
 * Used to
 */
public interface IClimateControlled {
	void addTemperatureChange(byte steps);

	void addHumidityChange(byte steps);
}
