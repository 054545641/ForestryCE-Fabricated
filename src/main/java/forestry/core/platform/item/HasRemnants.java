package forestry.core.platform.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tools that leave a broken remnant item in the player's inventory when they break.
 * 26.2: pickaxes, shovels and swords are plain {@link Item}s configured through
 * {@link Item.Properties#pickaxe}/{@link Item.Properties#shovel}/{@link Item.Properties#sword};
 * only axes and hoes still have dedicated item classes.
 */
public interface HasRemnants {
	static int damageWithRemnants(ItemStack stack, int amount, LivingEntity entity, Supplier<ItemStack> remnants) {
		if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
			if (entity instanceof Player player) {
				// make sure it's really broken
				stack.shrink(1);
				player.getInventory().add(remnants.get());
			}
		}
		return amount;
	}

	class Pickaxe extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Pickaxe(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.pickaxe(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
			return HasRemnants.damageWithRemnants(stack, super.damageItem(stack, amount, entity, onBroken), entity, this.remnants);
		}
	}

	class Shovel extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Shovel(ToolMaterial material, float damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.shovel(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
			return HasRemnants.damageWithRemnants(stack, super.damageItem(stack, amount, entity, onBroken), entity, this.remnants);
		}
	}

	class Axe extends AxeItem implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Axe(ToolMaterial material, float damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(material, damageBonus, speedModifier, properties);
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
			return HasRemnants.damageWithRemnants(stack, super.damageItem(stack, amount, entity, onBroken), entity, this.remnants);
		}
	}

	class Sword extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Sword(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.sword(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
			return HasRemnants.damageWithRemnants(stack, super.damageItem(stack, amount, entity, onBroken), entity, this.remnants);
		}
	}

	class Hoe extends HoeItem implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Hoe(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(material, damageBonus, speedModifier, properties);
			this.remnants = remnants;
		}

		@Override
		public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
			return HasRemnants.damageWithRemnants(stack, super.damageItem(stack, amount, entity, onBroken), entity, this.remnants);
		}
	}
}
