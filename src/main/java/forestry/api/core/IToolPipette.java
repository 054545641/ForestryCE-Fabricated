package forestry.api.core;

import net.minecraft.world.item.ItemStack;

/**
 * Taken from BuildCraft 5.0.x
 */
public interface IToolPipette {
	/**
	 * @return true if the pipette can pipette.
	 */
	boolean canPipette(ItemStack pipette);
}
