package forestry.core.platform.fluids;

import forestry.api.core.machines.*;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeMap;

import java.util.Set;
import java.util.function.Function;

public class FluidRecipeFilter extends ReloadableFluidFilter {
	public static final FluidRecipeFilter CARPENTER_INPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, FactoryRecipeTypes.CARPENTER.type(), ICarpenterRecipe::getInputFluid));
	public static final FluidRecipeFilter FERMENTER_INPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, FactoryRecipeTypes.FERMENTER.type(), IFermenterRecipe::getInputFluid));
	public static final FluidRecipeFilter FERMENTER_OUTPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluids(manager, FactoryRecipeTypes.FERMENTER.type(), IFermenterRecipe::getOutput));
	public static final FluidRecipeFilter FABRICATOR_SMELTING_OUTPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, FactoryRecipeTypes.FABRICATOR_SMELTING.type(), IFabricatorSmeltingRecipe::getResultFluid));
	public static final FluidRecipeFilter STILL_INPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, FactoryRecipeTypes.STILL.type(), IStillRecipe::getInput));
	public static final FluidRecipeFilter STILL_OUTPUT = new FluidRecipeFilter(manager -> RecipeUtils.getTargetFluidsFromStacks(manager, FactoryRecipeTypes.STILL.type(), IStillRecipe::getOutput));

	public FluidRecipeFilter(Function<RecipeMap, Set<Identifier>> filters) {
		super(() -> filters.apply(RecipeUtils.getRecipeMap()));
	}
}
