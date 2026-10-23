package forestry.api.core.machines;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import forestry.api.core.fluids.FluidVolume;

public interface ICarpenterRecipe extends IForestryRecipe {
	/**
	 * @return Number of work cycles required to craft the recipe once.
	 */
	int getPackagingTime();

	/**
	 * @return the crafting grid recipe. The crafting recipe's getRecipeOutput() is used as the ICarpenterRecipe's output.
	 */
	CraftingRecipe getCraftingGridRecipe();

	/**
	 * @return The required box, or an empty optional when no box is required
	 * Examples of boxes are the Forestry cartons and crates.
	 */
	Optional<Ingredient> getBox();

	/**
	 * @return the fluid required for this recipe. return {@link FluidVolume#EMPTY} if there is no required fluid.
	 */
	FluidVolume getInputFluid();

	boolean matches(FluidVolume fluid, ItemStack boxStack, Container craftingInventory, Level level);
}
