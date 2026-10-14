package forestry.apiculture.bees;

import forestry.core.platform.config.Constants;
import forestry.core.platform.item.ItemForestryFood;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

public class ItemAmbrosia extends ItemForestryFood {

	public ItemAmbrosia(Properties properties) {
		super(properties
			.food(new FoodProperties.Builder()
				.alwaysEdible()
				.nutrition(Constants.FOOD_AMBROSIA_HEAL)
				.saturationModifier(0.6f)
				.build(), Consumables.defaultDrink()
				.onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0)))
				.build()));
	}

	@Override
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

}
