package forestry.core.platform.item;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;

public class ItemForestryFood extends Item {
	public ItemForestryFood(Item.Properties properties) {
		super(properties);
	}

	public ItemForestryFood(int heal, float saturation, int useTime, Item.Properties properties) {
		super(properties.food(new FoodProperties.Builder().nutrition(heal).saturationModifier(saturation).build(),
			Consumables.defaultFood().consumeSeconds(useTime / 20.0f).build()));
	}

	public ItemForestryFood(int heal, float saturation, Item.Properties properties) {
		this(heal, saturation, 32, properties);
	}
}
