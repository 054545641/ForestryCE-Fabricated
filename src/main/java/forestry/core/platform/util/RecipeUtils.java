package forestry.core.platform.util;

import forestry.api.core.machines.*;
import forestry.core.platform.client.ClientsideCode;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.content.machines.features.FactoryRecipeTypes;
import forestry.core.content.machines.recipes.SmelterRecipe;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.content.worktable.inventory.WorktableCraftingContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import forestry.api.core.fluids.FluidVolume;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecipeUtils {
    public static RecipeMap getRecipeMap() { return forestry.core.platform.recipes.RecipeManagers.getRecipeMap(); }
    public static RecipeMap getRecipeMap(Level level) { return getRecipeMap(); }

	public static RegistryAccess getRegistryAccess() {
		MinecraftServer server = forestry.core.platform.data.LogicalData.isServer() ? forestry.core.platform.data.LogicalData.server() : null;
		return server == null ? (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT ? ClientsideCode.getRegistryAccess() : null) : server.registryAccess();
	}

	public static Registry<Fluid> getFluidRegistry() {
		MinecraftServer server = forestry.core.platform.data.LogicalData.isServer() ? forestry.core.platform.data.LogicalData.server() : null;
		return server == null ? (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT ? ClientsideCode.getFluidRegistry() : null) : server.registryAccess().lookupOrThrow(Registries.FLUID);
	}

	@Nullable
	public static <I extends RecipeInput, T extends Recipe<I>> RecipeHolder<T> getRecipe(@Nullable Identifier name) {
		if (name == null) {
			return null;
		}
		RecipeMap manager = getRecipeMap();
		if (manager == null) {
			return null;
		}
		return (RecipeHolder<T>) manager.byKey(ResourceKey.create(Registries.RECIPE, name));
	}

	public static <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> getRecipes(RecipeType<T> recipeType, I inventory, @Nullable Level world) {
		RecipeMap manager = getRecipeMap();
		if (manager == null || world == null) {
			return Collections.emptyList();
		}
		return manager.getRecipesFor(recipeType, inventory, world).toList();
	}

	public static List<RecipeHolder<CraftingRecipe>> findMatchingRecipes(CraftingInput inventory, Level level) {
		return getRecipeMap(level).getRecipesFor(RecipeType.CRAFTING, inventory, level).toList();
	}

	// Returns a crafting matrix for a certain recipe using available items
	@Nullable
	public static WorktableCraftingContainer getUsedMatrix(WorktableCraftingContainer originalMatrix, NonNullList<ItemStack> availableItems, Level level, CraftingRecipe recipe) {
		CraftingInput input = originalMatrix.asCraftInput();

		if (!recipe.matches(input, level)) {
			return null;
		}

		ItemStack expectedOutput = recipe.assemble(input);
		if (expectedOutput.isEmpty()) {
			return null;
		}

		WorktableCraftingContainer usedMatrix = new WorktableCraftingContainer();
		List<ItemStack> stockCopy = ItemStackUtil.condenseStacks(availableItems);

		for (int slot = 0; slot < originalMatrix.getContainerSize(); slot++) {
			ItemStack stack = originalMatrix.getItem(slot);

			if (!stack.isEmpty()) {
				ItemStack equivalent = getCraftingEquivalent(stockCopy, originalMatrix, slot, level, recipe, expectedOutput);
				if (equivalent.isEmpty()) {
					return null;
				} else {
					usedMatrix.setItem(slot, equivalent);
				}
			}
		}

		CraftingInput usedInput = usedMatrix.asCraftInput();

		if (recipe.matches(usedInput, level)) {
			ItemStack output = recipe.assemble(usedInput);
			if (ItemStack.matches(output, expectedOutput)) {
				return usedMatrix;
			}
		}

		return null;
	}

	private static ItemStack getCraftingEquivalent(List<ItemStack> stockCopy, WorktableCraftingContainer originalMatrix, int slot, Level level, CraftingRecipe recipe, ItemStack expectedOutput) {
		ItemStack originalStack = originalMatrix.getItem(slot);
		CraftingInput input = originalMatrix.asCraftInput();

		for (ItemStack stockStack : stockCopy) {
			if (!stockStack.isEmpty()) {
				ItemStack singleStockStack = stockStack.copy();
				singleStockStack.setCount(1);
				originalMatrix.setItem(slot, singleStockStack);

				if (recipe.matches(input, level)) {
					ItemStack output = recipe.assemble(input);
					if (ItemStack.matches(output, expectedOutput)) {
						originalMatrix.setItem(slot, originalStack);
						return stockStack.split(1);
					}
				}
			}
		}
		originalMatrix.setItem(slot, originalStack);
		return ItemStack.EMPTY;
	}

	@Nullable
	public static IFermenterRecipe getFermenterRecipe(RecipeMap manager, ItemStack inputItem, FluidVolume inputFluid) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.FERMENTER, recipe -> recipe.matches(inputItem, inputFluid));
	}

	public static boolean isFermenterInput(RecipeMap manager, ItemStack stack) {
		return getRecipes(manager, FactoryRecipeTypes.FERMENTER)
			.anyMatch(recipe -> recipe.getInputItem().filter(ingredient -> ingredient.test(stack)).isPresent());
	}

	@Nullable
	public static ICarpenterRecipe getCarpenterRecipe(RecipeMap manager, FluidVolume fluid, ItemStack boxStack, Container craftingInventory, Level level) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.CARPENTER, recipe -> recipe.matches(fluid, boxStack, craftingInventory, level));
	}

	public static boolean isCarpenterBox(RecipeMap manager, ItemStack stack) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.CARPENTER, recipe -> recipe.getBox().filter(ingredient -> ingredient.test(stack)).isPresent()) != null;
	}

	// Returns true if the item is part of any squeezer recipe.
	public static boolean isSqueezerIngredient(RecipeMap manager, ItemStack stack) {
		return getRecipes(manager, FactoryRecipeTypes.SQUEEZER).anyMatch(recipe -> {
			for (Ingredient ingredient : recipe.getInputs()) {
				if (ingredient.test(stack)) {
					return true;
				}
			}
			return false;
		});
	}

	@Nullable
	public static ISqueezerContainerRecipe getSqueezerContainerRecipe(RecipeMap manager, ItemStack stack) {
		if (!FluidHelper.isDrainableFilledContainer(stack)) {
			return null;
		}
		return getMatchingRecipe(manager, FactoryRecipeTypes.SQUEEZER_CONTAINER, recipe -> ItemStack.isSameItem(recipe.getEmptyContainer(), stack));
	}

	@Nullable
	public static ICentrifugeRecipe getCentrifugeRecipe(RecipeMap manager, ItemStack stack) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.CENTRIFUGE, recipe -> recipe.getInput().test(stack));
	}

	@Nullable
	public static IFabricatorSmeltingRecipe getFabricatorMeltingRecipe(RecipeMap manager, ItemStack stack) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.FABRICATOR_SMELTING, recipe -> recipe.getInput().test(stack));
	}

	@Nullable
	public static IFabricatorRecipe getFabricatorRecipe(RecipeMap manager, Level level, FluidVolume liquid, ItemStack stack, Container inventory) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.FABRICATOR, recipe -> recipe.matches(level, liquid, stack, inventory));
	}

	public static boolean isFabricatorPlan(RecipeMap manager, ItemStack stack) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.FABRICATOR, recipe -> recipe.getPlan().filter(ingredient -> ingredient.test(stack)).isPresent()) != null;
	}

	@Nullable
	public static IMoistenerRecipe getMoistenerRecipe(RecipeMap manager, ItemStack stack) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.MOISTENER, recipe -> recipe.getInput().test(stack));
	}

	@Nullable
	public static ISmelterRecipe getSmelterRecipe(RecipeMap manager, List<ItemStack> contents) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.SMELTER, recipe -> SmelterRecipe.canAlloy(recipe, contents));
	}

	@Nullable
	public static ISqueezerRecipe getSqueezerRecipe(RecipeMap manager, List<ItemStack> inputs) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.SQUEEZER, recipe -> ItemStackUtil.createConsume(recipe.getInputs(), inputs.size(), inputs::get, false).length > 0);
	}

	@Nullable
	public static IStillRecipe getStillRecipe(RecipeMap manager, FluidVolume input) {
		return getMatchingRecipe(manager, FactoryRecipeTypes.STILL, recipe -> recipe.matches(input));
	}

	@Nullable
	public static <R extends IForestryRecipe> R getMatchingRecipe(RecipeMap manager, FeatureRecipeType<R> type, Predicate<R> matcher) {
		return getRecipes(manager, type)
			.filter(matcher)
			.findFirst()
			.orElse(null);
	}

	public static <R extends IForestryRecipe> Stream<R> getRecipes(RecipeMap manager, FeatureRecipeType<R> type) {
		return manager.byType(type.type()).stream().map(RecipeHolder::value);
	}

	public static <R extends Recipe<C>, C extends RecipeInput> Set<Identifier> getTargetFluidsFromIngredients(RecipeMap manager, RecipeType<R> type, Function<R, net.minecraft.core.HolderSet<Fluid>> targetFluid) {
		return getTargetFluidStreams(manager, type, recipe -> targetFluid.apply(recipe).stream().map(net.minecraft.core.Holder::value));
	}

	public static <R extends Recipe<I>, I extends RecipeInput> Set<Identifier> getTargetFluidsFromStacks(RecipeMap manager, RecipeType<R> type, Function<R, FluidVolume> targetFluid) {
		return getTargetFluidStreams(manager, type, recipe -> Stream.of(targetFluid.apply(recipe).getFluid()));
	}

	public static <R extends Recipe<I>, I extends RecipeInput> Set<Identifier> getTargetFluids(RecipeMap manager, RecipeType<R> type, Function<R, Fluid> targetFluid) {
		return getTargetFluidStreams(manager, type, recipe -> Stream.of(targetFluid.apply(recipe)));
	}

	private static <R extends Recipe<I>, I extends RecipeInput> Set<Identifier> getTargetFluidStreams(RecipeMap manager, RecipeType<R> type, Function<R, Stream<Fluid>> targetFluid) {
		return manager.byType(type).stream()
			.flatMap(holder -> targetFluid.apply(holder.value()).map(ModUtil::getRegistryName))
			.collect(Collectors.toSet());
	}

	@Nullable
	public static <R extends IForestryRecipe> R getRecipeByOutput(FeatureRecipeType<R> recipeType, RegistryAccess registryAccess, ItemStack output) {
		return getRecipes(getRecipeMap(), recipeType)
			.filter(recipe -> ItemStack.isSameItem(recipe.getResultItem(registryAccess), output))
			.findFirst()
			.orElse(null);
	}

	@Nullable
	public static <T extends Recipe<?>> T unwrap(@Nullable RecipeHolder<T> holder) {
		return holder == null ? null : holder.value();
	}
}
