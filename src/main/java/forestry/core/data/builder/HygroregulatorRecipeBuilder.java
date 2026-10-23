package forestry.core.data.builder;

import forestry.apiculture.recipes.HygroregulatorRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;

public class HygroregulatorRecipeBuilder {
	private FluidStack liquid;
	private int humiditySteps;
	private int temperatureSteps;
	private int retainTime;

	public HygroregulatorRecipeBuilder setLiquid(FluidStack liquid) {
		this.liquid = liquid;
		return this;
	}

	public HygroregulatorRecipeBuilder setHumiditySteps(int humiditySteps) {
		this.humiditySteps = humiditySteps;
		return this;
	}

	public HygroregulatorRecipeBuilder setTemperatureSteps(int temperatureSteps) {
		this.temperatureSteps = temperatureSteps;
		return this;
	}

	public HygroregulatorRecipeBuilder setRetainTime(int retainTime) {
		this.retainTime = retainTime;
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new HygroregulatorRecipe(id, this.liquid, this.retainTime, (byte) this.humiditySteps, (byte) this.temperatureSteps), null);
	}
}
