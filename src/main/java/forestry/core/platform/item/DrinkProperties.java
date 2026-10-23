package forestry.core.platform.item;

/**
 * Definition of drinking properties for the drinkable fluid containers.
 */
public class DrinkProperties {
	private final int healAmount;
	private final int maxItemUseDuration;
	private final float saturationModifier;

	public DrinkProperties(int healAmount, float saturationModifier, int maxItemUseDuration) {
		this.healAmount = healAmount;
		this.saturationModifier = saturationModifier;
		this.maxItemUseDuration = maxItemUseDuration;
	}

	public int getHealAmount() {
		return this.healAmount;
	}

	public float getSaturationModifier() {
		return this.saturationModifier;
	}

	public int getMaxItemUseDuration() {
		return this.maxItemUseDuration;
	}
}
