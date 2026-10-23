package forestry.fabric.test;

import forestry.Forestry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Test-only observer of the optional production integration. */
public final class F1JeiProbe implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override public Identifier getPluginUid() { return F1RegistrationTests.id("f1_test"); }
    @Override public void onRuntimeAvailable(IJeiRuntime available) { runtime = available; }
    @Override public void onRuntimeUnavailable() { runtime = null; }
    public static boolean isReady() { return runtime != null; }

    public static void verifyGeneticIngredients() {
        var stacks = runtime.getIngredientManager().getAllItemStacks();
        F3WorldTests.verifyGeneticVariants(stacks, forestry.core.platform.util.SpeciesUtil.BEE_TYPE.get(), "JEI bees");
        F3WorldTests.verifyGeneticVariants(stacks, forestry.core.platform.util.SpeciesUtil.TREE_TYPE.get(), "JEI trees");
        var butterflies = forestry.core.platform.util.SpeciesUtil.getButterflyTypeSafe();
        if (butterflies != null) F3WorldTests.verifyGeneticVariants(stacks, butterflies, "JEI butterflies");
        Forestry.LOGGER.info("FORESTRY_GENETIC_JEI_VERIFIED");
    }

    public static void showApatiteRecipe() {
        var recipes = runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).get()
            .filter(recipe -> recipe.id().identifier().equals(F1RegistrationTests.id("apatite_block"))).toList();
        F1RegistrationTests.require(recipes.size() == 1, "JEI must expose the Forestry apatite compression recipe");
        runtime.getIngredientFilter().setFilterText("@forestry");
        runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING), recipes, List.of());
        Forestry.LOGGER.info("FORESTRY_F2_JEI_RECIPE_VERIFIED forestry:apatite_block");
    }

    public static List<String> forestryCategories() {
        return runtime.getRecipeManager().createRecipeCategoryLookup().get()
            .map(category -> category.getRecipeType().getUid()).filter(id -> id.getNamespace().equals("forestry"))
            .map(Identifier::getPath).sorted().toList();
    }

    public static <Recipe> void showForestryCategory(String name) {
        @SuppressWarnings("unchecked")
        var type = (mezz.jei.api.recipe.types.IRecipeType<Recipe>) runtime.getRecipeManager().getRecipeType(F1RegistrationTests.id(name)).orElseThrow();
        var recipes = runtime.getRecipeManager().createRecipeLookup(type).get().toList();
        F1RegistrationTests.require(!recipes.isEmpty(), "Empty Forestry JEI category: " + name);
        var category = runtime.getRecipeManager().getRecipeCategory(type);
        for (var recipe : recipes) {
            F1RegistrationTests.require(runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
                runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).isPresent(), "Invalid JEI layout in " + name);
        }
        Forestry.LOGGER.info("FORESTRY_F3_JEI_LAYOUTS_VERIFIED category={} recipes={}", name, recipes.size());
        runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(type), recipes, List.of());
    }

    public static void transferCarpenter(net.minecraft.client.Minecraft client) {
        var type = forestry.core.platform.recipes.jei.ForestryRecipeType.CARPENTER;
        var category = runtime.getRecipeManager().getRecipeCategory(type);
        var recipe = runtime.getRecipeManager().createRecipeLookup(type).get().findFirst().orElseThrow();
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
            runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
        var menu = (forestry.core.content.machines.gui.ContainerCarpenter) client.player.containerMenu;
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category).orElseThrow();
        F1RegistrationTests.require(handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), client.player, false, true) == null,
            "Real JEI carpenter transfer rejected");
    }

    public static void transferFabricator(net.minecraft.client.Minecraft client) {
        var type = forestry.core.platform.recipes.jei.ForestryRecipeType.FABRICATOR;
        var category = runtime.getRecipeManager().getRecipeCategory(type);
        var recipe = runtime.getRecipeManager().createRecipeLookup(type).get().findFirst().orElseThrow();
        var layout = runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
            runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
        var menu = (forestry.core.content.machines.gui.ContainerFabricator) client.player.containerMenu;
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category).orElseThrow();
        F1RegistrationTests.require(handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), client.player, false, true) == null,
            "Real JEI fabricator transfer rejected");
    }
}
