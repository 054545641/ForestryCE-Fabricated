package forestry.api.core.machines;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public interface IMoistenerRecipe extends IForestryRecipe {
	/**
	 * Moistener runs at 1 - 4 time ticks per ingame tick depending on light level. For mycelium this value is currently 5000.
	 *
	 * @return moistener ticks to process one item.
	 */
	int getTimePerItem();

	/**
	 * @return Item required in resource stack. Will be reduced by one per produced item.
	 */
	Ingredient getInput();

	/**
	 * @return Item to produce per resource processed.
	 */
	ItemStack getProduct();
}
