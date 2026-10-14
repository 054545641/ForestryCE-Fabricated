package forestry.core.data.builder;

import java.util.Optional;
import forestry.core.content.machines.recipes.CarpenterRecipe;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;

public class CarpenterRecipeBuilder {
	private static final Criterion<ImpossibleTrigger.TriggerInstance> IMPOSSIBLE = new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance());

	private int packagingTime = 5;
	@Nullable
	private FluidStack liquid;
	private Optional<Ingredient> box = Optional.empty();
	private CraftingRecipe recipe;
	@Nullable
	private ItemStack result;

	public CarpenterRecipeBuilder setPackagingTime(int packagingTime) {
		this.packagingTime = packagingTime;
		return this;
	}

	public CarpenterRecipeBuilder setLiquid(@Nullable FluidStack liquid) {
		this.liquid = liquid;
		return this;
	}

	public CarpenterRecipeBuilder setBox(Ingredient box) {
		this.box = Optional.of(box);
		return this;
	}

	public CarpenterRecipeBuilder recipe(ShapedRecipeBuilder recipe) {
		RecipeCapture capture = new RecipeCapture();
		recipe.unlockedBy("impossible", IMPOSSIBLE).save(capture, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.withDefaultNamespace("forestry_carpenter_shaped")));
		this.recipe = capture.recipe(Identifier.withDefaultNamespace("forestry_carpenter_shaped"), CraftingRecipe.class);
		return this;
	}

	public CarpenterRecipeBuilder recipe(ShapelessRecipeBuilder recipe) {
		RecipeCapture capture = new RecipeCapture();
		recipe.unlockedBy("impossible", IMPOSSIBLE).save(capture, net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Identifier.withDefaultNamespace("forestry_carpenter_shapeless")));
		this.recipe = capture.recipe(Identifier.withDefaultNamespace("forestry_carpenter_shapeless"), CraftingRecipe.class);
		return this;
	}

	/**
	 * In case the recipe should create an item stack, and not a basic item without NBT
	 *
	 * @param result The result to override {@link #recipe(ShapedRecipeBuilder)}
	 * @return This builder for chaining
	 */
	public CarpenterRecipeBuilder override(ItemStack result) {
		this.result = result;
		return this;
	}

	public void build(RecipeOutput output, Identifier id) {
		output.accept(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id), new CarpenterRecipe(id, this.packagingTime, this.liquid == null ? FluidStack.EMPTY : this.liquid, this.box, this.recipe, this.result), null);
	}
}
