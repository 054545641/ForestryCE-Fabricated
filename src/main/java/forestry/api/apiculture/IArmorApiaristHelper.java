package forestry.api.apiculture;

import forestry.api.apiculture.genetics.IBeeEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Helper class for checking whether an entity is wearing Apiarist Armor
 *
 * @author Vexatos
 */
public interface IArmorApiaristHelper {
	/**
	 * Called when the apiarist's armor acts as protection against an attack.
	 *
	 * @param stack     ItemStack to check
	 * @param entity    Entity being attacked
	 * @param cause     Optional cause of attack, such as a bee effect identifier
	 * @param doProtect Whether or not to actually do the side effects of protection
	 * @return Whether or not the item is valid Apiarist Armor and should protect the player from that attack
	 * @since Forestry 4.2
	 */
	boolean isArmorApiarist(ItemStack stack, LivingEntity entity, IBeeEffect cause, boolean doProtect);

	/**
	 * Called when the apiarist's armor acts as protection against an attack.
	 *
	 * @param entity    Entity being attacked
	 * @param cause     Optional cause of attack, such as a bee effect identifier
	 * @param doProtect Whether or not to actually do the side effects of protection
	 * @return The number of valid Apiarist Armor pieces the player is wearing that are actually protecting.
	 * 4 means full protection, but it can go higher if they are holding items like the smoker.
	 * @since Forestry 4.2
	 */
	int wearsItems(LivingEntity entity, @Nullable IBeeEffect cause, boolean doProtect);
}
