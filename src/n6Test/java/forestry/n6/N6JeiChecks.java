package forestry.n6;

import com.mojang.logging.LogUtils;
import forestry.api.core.machines.ICarpenterRecipe;
import forestry.core.platform.recipes.jei.ForestryRecipeType;
import forestry.core.platform.util.JeiUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static forestry.n6.N6ReleaseTest.check;

/** This probe is discovered only by JEI; the normal release probe never resolves JEI signatures. */
@JeiPlugin
public final class N6JeiChecks implements IModPlugin {
    static IJeiRuntime runtime;
    static int generation;
    static List<IRecipeCategory<?>> categories;
    private static IRecipeCategory selectedCategory;
    private static Object selectedRecipe;

    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath(N6ReleaseTest.ID, "jei"); }
    @Override public void onRuntimeAvailable(IJeiRuntime value) { runtime = value; generation++; }
    @Override public void onRuntimeUnavailable() { runtime = null; }

    static boolean ready() { return runtime != null; }

    static void audit() {
        var manager = runtime.getRecipeManager();
        check(manager.getRecipeCategory(ForestryRecipeType.CARPENTER).getTitle().getString()
            .equals(net.minecraft.network.chat.Component.translatable("block.forestry.carpenter").getString()), "JEI title follows current language after reload");
        categories = manager.createRecipeCategoryLookup().includeHidden().get()
            .filter(category -> category.getRecipeType().getUid().getNamespace().equals("forestry")).toList();
        check(categories.size() >= 14, "All core JEI categories register: " + categories.size());
        int layouts = 0;
        for (var category : categories) layouts += auditCategory(category);
        var ingredientHelper = runtime.getIngredientManager().getIngredientHelper(mezz.jei.api.constants.VanillaTypes.ITEM_STACK);
        for (var type : forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
            var species = type.getAllSpecies().stream().limit(2).toList();
            var lifeStage = type.getLifeStages().iterator().next();
            check(!ingredientHelper.getUid(species.get(0).createStack(lifeStage), mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)
                .equals(ingredientHelper.getUid(species.get(1).createStack(lifeStage), mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)), "JEI distinguishes species: " + type.id());
        }
        for (var item : forestry.core.features.FluidsItems.CONTAINERS.itemArray()) {
            var water = new ItemStack(item);
            water.set(forestry.core.features.CoreDataComponents.FLUID_CONTENT, net.neoforged.neoforge.fluids.SimpleFluidContent.copyOf(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)));
            check(!ingredientHelper.getUid(water, mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)
                .equals(ingredientHelper.getUid(new ItemStack(item), mezz.jei.api.ingredients.subtypes.UidContext.Ingredient)), "JEI distinguishes filled and empty containers");
        }
        LogUtils.getLogger().info("FORESTRY_N6_JEI_READY categories={} layouts={}", categories.size(), layouts);
    }

    private static <T> int auditCategory(IRecipeCategory<T> category) {
        var manager = runtime.getRecipeManager();
        var recipes = manager.createRecipeLookup(category.getRecipeType()).includeHidden().get().toList();
        check(!recipes.isEmpty(), "Populated JEI category: " + category.getRecipeType().getUid());
        for (T recipe : recipes) {
            var layout = manager.createRecipeLayoutDrawable(category, recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
            check(layout.isPresent(), "JEI layout builds: " + category.getRecipeType().getUid() + "/" + recipe);
        }
        LogUtils.getLogger().info("FORESTRY_N6_JEI_CATEGORY {} recipes={}", category.getRecipeType().getUid(), recipes.size());
        return recipes.size();
    }

    static int categoryCount() { return categories.size(); }
    static String show(int index) {
        var type = categories.get(index).getRecipeType();
        runtime.getRecipesGui().showTypes(List.of(type));
        check(Minecraft.getInstance().gui.screen() != null, "Real JEI recipe screen opened");
        return type.getUid().getPath().replace('.', '_');
    }

    @SuppressWarnings({"unchecked", "rawtypes", "removal"})
    static NonNullList<ItemStack> transfer(int fixture, boolean execute) {
        var client = Minecraft.getInstance();
        var manager = runtime.getRecipeManager();
        var type = fixture < 2 ? ForestryRecipeType.CARPENTER : fixture == 2 ? ForestryRecipeType.FABRICATOR : RecipeTypes.CRAFTING;
        IRecipeCategory category = manager.getRecipeCategory(type);
        List<?> recipes = manager.createRecipeLookup(type).get().toList();
        Object recipe;
        if (fixture < 2) recipe = recipes.stream().filter(r -> ((ICarpenterRecipe) r).getBox().isPresent() == (fixture == 1)).findFirst().orElseThrow();
        else if (fixture == 3) recipe = recipes.stream().filter(r -> ((net.minecraft.world.item.crafting.RecipeHolder<?>) r).id().identifier().getPath().equals("crafting_table")).findFirst().orElseThrow();
        else recipe = recipes.getFirst();
        selectedCategory = category;
        selectedRecipe = recipe;
        var layout = manager.createRecipeLayoutDrawable(category, recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
        var slots = ((mezz.jei.api.gui.IRecipeLayoutDrawable<?>) layout).getRecipeSlotsView();
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(client.player.containerMenu, category).orElseThrow();
        var error = ((mezz.jei.api.recipe.transfer.IRecipeTransferHandler) handler).transferRecipe(client.player.containerMenu, recipe, slots, client.player, false, execute);
        check(error == null, "Production recipe transfer accepted fixture=" + fixture);
        return fixture == 3 ? forestry.core.platform.compat.jei.JeiUtil.getFirstItemStacks(slots) : JeiUtil.getCraftingItems(slots);
    }

    @SuppressWarnings("unchecked")
    static void showTransfer() {
        runtime.getRecipesGui().showRecipes(selectedCategory, List.of(selectedRecipe), List.of());
    }

    static NonNullList<ItemStack> clickTransfer(int fixture) throws Exception {
        var screen = Minecraft.getInstance().gui.screen();
        var layoutsField = screen.getClass().getDeclaredField("layouts"); layoutsField.setAccessible(true);
        var layouts = layoutsField.get(screen);
        var valuesField = layouts.getClass().getDeclaredField("recipeLayoutsWithButtons"); valuesField.setAccessible(true);
        var entry = ((List<?>) valuesField.get(layouts)).getFirst();
        var layout = (mezz.jei.api.gui.IRecipeLayoutDrawable<?>) entry.getClass().getMethod("getRecipeLayout").invoke(entry);
        var slots = layout.getRecipeSlotsView();
        var expected = fixture == 3 ? forestry.core.platform.compat.jei.JeiUtil.getFirstItemStacks(slots) : JeiUtil.getCraftingItems(slots);
        var button = layout.getRecipeTransferButtonArea();
        var rect = layout.getRect();
        var event = new net.minecraft.client.input.MouseButtonEvent(rect.getX() + button.getX() + button.getWidth() / 2.,
            rect.getY() + button.getY() + button.getHeight() / 2., new net.minecraft.client.input.MouseButtonInfo(0, 0));
        check(screen.mouseClicked(event, false), "Native JEI transfer button accepts mouse press");
        check(screen.mouseReleased(event), "Native JEI transfer button accepts mouse release");
        return expected;
    }
}
