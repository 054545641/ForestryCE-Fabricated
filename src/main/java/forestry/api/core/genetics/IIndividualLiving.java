package forestry.api.core.genetics;

import net.minecraft.world.level.Level;

/**
 * An individual with health and a lifespan.
 */
public interface IIndividualLiving extends IIndividual {
	/**
	 * @return Current health of the individual.
	 */
	int getHealth();

	/**
	 * Set the current health of the individual.
	 */
	void setHealth(int health);

	/**
	 * @return Maximum health of the individual.
	 */
	int getMaxHealth();

	/**
	 * Age the individual.
	 *
	 * @param level   The world where the individual lives.
	 * @param ageStep The amount to age this by. Base amount is {@code 1f}, and higher values should age faster.
	 *                {@code 0f} should not age, and negative values will instantly kill this individual. If given as
	 *                a decimal number, the bee is aged by the whole number of steps, and has a percentage chance to age
	 *                based on the decimal portion. For example, when ageStep = 2.3, the bee is aged twice, with a 30%
	 *                chance to age a third time.
	 */
	void age(Level level, float ageStep);

	/**
	 * @return true if the individual is among the living.
	 */
	boolean isAlive();

	@Override
	IIndividualLiving copy();
}
