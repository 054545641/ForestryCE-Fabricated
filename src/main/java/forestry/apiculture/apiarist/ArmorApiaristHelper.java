package forestry.apiculture.apiarist;

import forestry.api.ForestryCapabilities;
import forestry.api.apiculture.IArmorApiaristHelper;
import forestry.api.apiculture.IBeeProtection;
import forestry.api.apiculture.genetics.IBeeEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class ArmorApiaristHelper implements IArmorApiaristHelper {
	@Override
	public boolean isArmorApiarist(ItemStack stack, LivingEntity entity, IBeeEffect cause, boolean doProtect) {
		if (stack.isEmpty()) {
			return false;
		}

		IBeeProtection armorApiarist = ForestryCapabilities.BEE_PROTECTION.find(stack, null);
		return armorApiarist != null && armorApiarist.protectEntity(entity, stack, cause, doProtect);
	}

	@Override
	public int wearsItems(LivingEntity entity, @Nullable IBeeEffect cause, boolean doProtect) {
		int count = 0;

		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack armorItem = entity.getItemBySlot(slot);
			if (isArmorApiarist(armorItem, entity, cause, doProtect)) {
				count++;
			}
		}

		return count;
	}
}
