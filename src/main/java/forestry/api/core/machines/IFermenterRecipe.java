package forestry.api.core.machines;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import forestry.api.core.fluids.FluidVolume;

public interface IFermenterRecipe extends IForestryRecipe {
	/**
	 * @return The required item resource, or an empty optional for fluid-only recipes
	 */
	Optional<Ingredient> getInputItem();

	/**
	 * @return FluidVolume representing the input fluid resource.
	 */
	FluidVolume getInputFluid();

	/**
	 * @return Value of the given resource, i.e. how much needs to be fermented for the output to be deposited into the product tank.
	 */
	int getFermentationValue();

	/**
	 * @return Modifies the amount of liquid output per work cycle.
	 * (water = 1.0f, honey = 1.5f)
	 */
	float getModifier();

	/**
	 * @return Fluid representing output. Amount is determined by fermentationValue * modifier.
	 */
	Fluid getOutput();

	boolean matches(ItemStack inputItem, FluidVolume inputFluid);
}
