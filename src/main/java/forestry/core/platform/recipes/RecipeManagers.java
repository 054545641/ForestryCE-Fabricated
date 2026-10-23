package forestry.core.platform.recipes;

import forestry.core.platform.data.LogicalData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.item.crafting.RecipeMap;

/** Recipes are scoped to the logical side and replaced only after a successful reload/synchronization. */
public final class RecipeManagers {
    private static final LogicalData<RecipeMap> RECIPES = new LogicalData<>(RecipeMap.EMPTY);
    private static final LogicalData<Integer> REVISIONS = new LogicalData<>(0);
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> replace(RecipeMap.create(server.getRecipeManager().getRecipes())));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) replace(RecipeMap.create(server.getRecipeManager().getRecipes()));
        });
    }
    public static RecipeMap getRecipeMap() { return RECIPES.get(); }
    public static void replace(RecipeMap map) { RECIPES.set(map); invalidateCaches(); }
    public static int getRecipeReloads() { return REVISIONS.get(); }
    public static void invalidateCaches() { REVISIONS.set(REVISIONS.get() + 1); }
}
