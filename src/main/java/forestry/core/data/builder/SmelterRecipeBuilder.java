package forestry.core.data.builder;

import com.google.common.base.Preconditions;
import forestry.core.content.machines.recipes.SmelterRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.ArrayList;
import java.util.List;

public class SmelterRecipeBuilder {
	private final List<SizedIngredient> ingredients = new ArrayList<>();
	private SizedIngredient output;
	private int processingTime;

	public SmelterRecipeBuilder setProcessingTime(int processingTime) {
		this.processingTime = processingTime;
		return this;
	}

	public SmelterRecipeBuilder addIngredient(Ingredient input, int amount) {
		this.ingredients.add(new SizedIngredient(input, amount));
		return this;
	}

	public SmelterRecipeBuilder addIngredient(Ingredient input) {
		return addIngredient(input, 1);
	}

	public SmelterRecipeBuilder setOutput(Ingredient output) {
		return setOutput(output, 1);
	}

	public SmelterRecipeBuilder setOutput(Ingredient output, int amount) {
		this.output = new SizedIngredient(output, amount);
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		Preconditions.checkState(!this.ingredients.isEmpty(), "Empty smelter recipes are not allowed");
		Preconditions.checkNotNull(this.output, "Smelter recipes need an output");
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new SmelterRecipe(id, this.ingredients, this.output, this.processingTime), null);
	}
}
