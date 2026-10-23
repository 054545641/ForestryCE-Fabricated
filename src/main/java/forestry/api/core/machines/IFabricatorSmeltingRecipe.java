package forestry.api.core.machines;

import net.minecraft.world.item.crafting.Ingredient;
import forestry.api.core.fluids.FluidVolume;

public interface IFabricatorSmeltingRecipe extends IForestryRecipe {
	/**
	 * @return item to be melted down
	 */
	Ingredient getInput();

	/**
	 * @return temperature at which the item melts. Glass is 1000, Sand is 3000.
	 */
	int getMeltingPoint();

	/**
	 * @return resulting fluid
	 */
	FluidVolume getResultFluid();
}
