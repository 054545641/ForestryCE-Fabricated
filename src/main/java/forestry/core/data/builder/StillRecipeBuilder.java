package forestry.core.data.builder;

import forestry.core.content.machines.recipes.StillRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

public class StillRecipeBuilder {

	private int timePerUnit;
	private FluidStack input;
	private FluidStack output;

	public StillRecipeBuilder setTimePerUnit(int timePerUnit) {
		this.timePerUnit = timePerUnit;
		return this;
	}

	public StillRecipeBuilder setInput(FluidStack input) {
		this.input = input;
		return this;
	}

	public StillRecipeBuilder setOutput(FluidStack output) {
		this.output = output;
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new StillRecipe(id, this.timePerUnit, this.input, this.output), null);
	}
}
