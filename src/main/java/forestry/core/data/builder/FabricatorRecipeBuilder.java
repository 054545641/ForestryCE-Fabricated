package forestry.core.data.builder;

import java.util.Optional;
import forestry.core.content.machines.recipes.FabricatorRecipe;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.fluids.FluidStack;

public class FabricatorRecipeBuilder {
	private static final Criterion<ImpossibleTrigger.TriggerInstance> IMPOSSIBLE = new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance());

	private Optional<Ingredient> plan = Optional.empty();
	private FluidStack molten;
	private ShapedRecipe recipe;

	public FabricatorRecipeBuilder setPlan(Ingredient plan) {
		this.plan = Optional.of(plan);
		return this;
	}

	public FabricatorRecipeBuilder setMolten(FluidStack molten) {
		this.molten = molten;
		return this;
	}

	public FabricatorRecipeBuilder recipe(ShapedRecipeBuilder recipe) {
		RecipeCapture capture = new RecipeCapture();
		recipe.unlockedBy("impossible", IMPOSSIBLE).save(capture, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.withDefaultNamespace("forestry_fabricator")));
		this.recipe = capture.recipe(Identifier.withDefaultNamespace("forestry_fabricator"), ShapedRecipe.class);
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new FabricatorRecipe(id, this.plan, this.molten, this.recipe), null);
	}
}
