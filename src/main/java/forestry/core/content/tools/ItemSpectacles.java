package forestry.core.content.tools;

import forestry.api.ForestryConstants;
import forestry.api.ForestryTags;
import forestry.api.core.ISpectacleVision;
import forestry.core.platform.capabilities.SpectacleVision;
import forestry.core.platform.util.ItemTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Naturalist's spectacles. 26.2: no more {@code ArmorItem}; the helmet behavior comes from
 * {@link Item.Properties#humanoidArmor} and the worn texture from a data-driven equipment asset
 * (replacing the old {@code getArmorTexture} override).
 */
public class ItemSpectacles extends forestry.core.platform.item.ItemForestry {
	public static final ResourceKey<EquipmentAsset> SPECTACLES_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, ForestryConstants.forestry("spectacles"));
	public static final ISpectacleVision SPECTACLE_VISION = SpectacleVision.INSTANCE;

	private static final ArmorMaterial SPECTACLES_MATERIAL = new ArmorMaterial(
		5,
		makeDefense(),
		15,
		SoundEvents.ARMOR_EQUIP_LEATHER,
		0.0F,
		0.0F,
		ForestryTags.Items.REPAIRS_APIARIST_ARMOR,
		SPECTACLES_ASSET
	);

	private static Map<ArmorType, Integer> makeDefense() {
		Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
		defense.put(ArmorType.HELMET, 1);
		return defense;
	}

	public ItemSpectacles(Properties properties) {
		super(properties.humanoidArmor(SPECTACLES_MATERIAL, ArmorType.HELMET).durability(100));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
		ItemTooltipUtil.addInformation(stack, tooltip);
	}
}
