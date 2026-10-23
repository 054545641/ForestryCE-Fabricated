package forestry.api.apiculture;

import forestry.api.core.genetics.pollen.IPollen;

public interface IBeeListener {
	/**
	 * Called when the bees wear out the housing's equipment.
	 *
	 * @param amount Integer indicating the amount worn out.
	 */
	default void wearOutEquipment(int amount) {
	}

	/**
	 * Called after the children have been spawned, and before the new princess is spawned.
	 */
	default void onQueenDeath() {
	}

	/**
	 * Called when the bees have retrieved some pollen.
	 *
	 * @return true if this bee listener consumed the pollen.
	 */
	default boolean onPollenRetrieved(IPollen<?> pollen) {
		return false;
	}
}
