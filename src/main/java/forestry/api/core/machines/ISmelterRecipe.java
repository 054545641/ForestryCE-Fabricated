package forestry.api.core.machines;

import net.minecraft.world.item.ItemStack;
import forestry.api.core.machines.CountedIngredient;

import java.util.List;

public interface ISmelterRecipe extends IForestryRecipe {
	/**
	 * @return Number of work cycles required to alloy one set of resources
	 */
	int getProcessingTime();

	/**
	 * @return Sized ingredients representing the required resources for one process
	 */
	List<CountedIngredient> getInputs();

	/**
	 * @return The alloy produced by one process
	 */
	ItemStack getOutput();
}
