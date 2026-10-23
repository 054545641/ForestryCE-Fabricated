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
    ItemStack getRemnant();

	class Pickaxe extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Pickaxe(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.pickaxe(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override public ItemStack getRemnant() { return this.remnants.get(); }
	}

	class Shovel extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Shovel(ToolMaterial material, float damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.shovel(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override public ItemStack getRemnant() { return this.remnants.get(); }
	}

	class Axe extends AxeItem implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Axe(ToolMaterial material, float damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(material, damageBonus, speedModifier, properties);
			this.remnants = remnants;
		}

		@Override public ItemStack getRemnant() { return this.remnants.get(); }
	}

	class Sword extends Item implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Sword(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(properties.sword(material, damageBonus, speedModifier));
			this.remnants = remnants;
		}

		@Override public ItemStack getRemnant() { return this.remnants.get(); }
	}

	class Hoe extends HoeItem implements HasRemnants {
		private final Supplier<ItemStack> remnants;

		public Hoe(ToolMaterial material, int damageBonus, float speedModifier, Properties properties, Supplier<ItemStack> remnants) {
			super(material, damageBonus, speedModifier, properties);
			this.remnants = remnants;
		}

		@Override public ItemStack getRemnant() { return this.remnants.get(); }
	}
}
