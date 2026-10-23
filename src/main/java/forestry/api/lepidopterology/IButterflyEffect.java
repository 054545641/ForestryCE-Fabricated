package forestry.api.lepidopterology;

import forestry.api.core.genetics.IEffectData;

/**
 * Unimplemented.
 */
public interface IButterflyEffect {
	/**
	 * @return Whether the allele for this value is dominant or recessive.
	 */
	boolean isDominant();

	/**
	 * Used by butterflies to trigger effects in the world.
	 *
	 * @param butterfly {@link IEntityButterfly}
	 * @return {@link forestry.api.core.genetics.IEffectData} for the next cycle.
	 */
	IEffectData doEffect(IEntityButterfly butterfly, IEffectData storedData);
}
