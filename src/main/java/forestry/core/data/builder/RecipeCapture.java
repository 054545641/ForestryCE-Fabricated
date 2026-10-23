package forestry.core.data.builder;

import com.google.common.base.Preconditions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

import javax.annotation.Nullable;

final class RecipeCapture implements RecipeOutput {
	@Nullable
	private Recipe<?> recipe;
	@Nullable
	private AdvancementHolder advancement;

	@Override
	public Advancement.Builder advancement() {
		return Advancement.Builder.recipeAdvancement();
	}

	@Override
	public void accept(net.minecraft.resources.ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement, ICondition... conditions) {
		this.recipe = recipe;
		this.advancement = advancement;
	}

	@Override
	public void includeRootAdvancement() {
		throw new UnsupportedOperationException("A captured machine sub-recipe has no root advancement");
	}

	public <T extends Recipe<?>> T recipe(Identifier id, Class<T> recipeClass) {
		Preconditions.checkState(this.recipe != null, "No recipe was captured for %s", id);
		Preconditions.checkState(recipeClass.isInstance(this.recipe), "Captured recipe for %s was %s, expected %s", id, this.recipe.getClass().getName(), recipeClass.getName());
		return recipeClass.cast(this.recipe);
	}

	@Nullable
	public AdvancementHolder advancementHolder() {
		return this.advancement;
	}
}
