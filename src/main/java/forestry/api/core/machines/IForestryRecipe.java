package forestry.api.core.machines;

import net.minecraft.resources.Identifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

public interface IForestryRecipe extends Recipe<RecipeInput> {
	// <editor-fold desc="Ignore these methods, we just piggy back off Minecraft's system for recipe sync">
	@Deprecated
	@Override
	default boolean matches(RecipeInput inv, Level level) {
		return false;
	}

	@Deprecated
	@Override
	default ItemStack assemble(RecipeInput inv) {
		return ItemStack.EMPTY;
	}

	@Deprecated
	@Override
	default boolean isSpecial() {
		return true;
	}

	@Deprecated
	@Override
	default boolean showNotification() {
		return false;
	}

	@Deprecated
	@Override
	default String group() {
		return "forestry";
	}

	@Deprecated
	@Override
	default PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Deprecated
	@Override
	default RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CRAFTING_MISC;
	}
	// </editor-fold>

	/** Returns the Forestry recipe preview without assembling a crafting input. */
	ItemStack getResultItem(HolderLookup.Provider registries);

	Identifier getId();
}
