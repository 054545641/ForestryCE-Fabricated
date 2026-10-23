package forestry.api.core.machines;

import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import forestry.api.core.fluids.FluidVolume;

public interface IFabricatorRecipe extends IForestryRecipe {
	/**
	 * @return the molten liquid (and amount) required for this recipe.
	 */
	FluidVolume getResultFluid();

	/**
	 * @return The required plan, or an empty optional when no plan is required
	 */
	Optional<Ingredient> getPlan();

	/**
	 * @return the crafting grid recipe. The crafting recipe's getRecipeOutput() is used as the IFabricatorRecipe's output.
	 */
	ShapedRecipe getCraftingGridRecipe();

	boolean matches(Level level, FluidVolume liquid, ItemStack stack, Container inventory);
}
