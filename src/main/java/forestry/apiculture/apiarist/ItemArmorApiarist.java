package forestry.apiculture.apiarist;

import forestry.api.ForestryConstants;
import forestry.api.ForestryTags;
import forestry.api.apiculture.IBeeProtection;
import forestry.api.apiculture.genetics.IBeeEffect;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Apiarist's armor. 26.2: no more {@code ArmorItem}; armor is a plain item whose properties get
 * armor behavior from {@link Item.Properties#humanoidArmor(ArmorMaterial, ArmorType)} and whose
 * look comes from a data-driven {@link EquipmentAsset}.
 */
public class ItemArmorApiarist extends Item {
	public static final ResourceKey<EquipmentAsset> APIARIST_ARMOR_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ForestryConstants.forestry("apiarist_armor"));

	private static final ArmorMaterial APIARIST_ARMOR_MATERIAL = new ArmorMaterial(
		15,
		makeDefense(1, 2, 3, 1),
		15,
		SoundEvents.ARMOR_EQUIP_LEATHER,
		0.0F,
		0.0F,
		ForestryTags.Items.REPAIRS_APIARIST_ARMOR,
		APIARIST_ARMOR_ASSET
	);

	private static Map<ArmorType, Integer> makeDefense(int boots, int leggings, int chestplate, int helmet) {
		Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
		defense.put(ArmorType.BOOTS, boots);
		defense.put(ArmorType.LEGGINGS, leggings);
		defense.put(ArmorType.CHESTPLATE, chestplate);
		defense.put(ArmorType.HELMET, helmet);
		return defense;
	}

	public enum BeeProtection implements IBeeProtection {
		INSTANCE;

		@Override
		public boolean protectEntity(LivingEntity entity, ItemStack armor, @Nullable IBeeEffect cause, boolean doProtect) {
			return true;
		}
	}

	public ItemArmorApiarist(ArmorType type, Properties properties) {
		super(properties.humanoidArmor(APIARIST_ARMOR_MATERIAL, type));
	}
}
