package forestry;

import forestry.modules.ForestryModuleManager;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Fabric entrypoint. Common initialization never touches client classes. */
public final class Forestry implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("forestry");
    public static final boolean DEBUG = net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();

    @Override
    public void onInitialize() {
        forestry.core.platform.data.LogicalData.initializeLifecycle();
        net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents.TAGS_LOADED.register((registries, clientSide) ->
            registries.lookup(net.minecraft.core.registries.Registries.BIOME).ifPresent(biomes ->
                ((forestry.core.engine.climate.ForestryClimateManager) forestry.api.IForestryApi.INSTANCE.getClimateManager()).onBiomesReloaded(biomes, clientSide)));
        ForestryModuleManager.INSTANCE.init();
        forestry.core.platform.commands.ForestryFabricCommands.initialize();
        forestry.core.platform.fluids.ForestryFluidAttributes.initialize();
        forestry.core.platform.fluids.MilkFluid.initializeTransfer();
        forestry.core.platform.multiblock.MultiblockEventHandler.initialize();
        forestry.core.platform.transfer.WorldWorkQueue.initialize();
        forestry.core.platform.config.ForestryConfig.initialize();
        forestry.core.platform.data.FabricDomainBootstrap.initialize();
        forestry.core.platform.worldgen.ForestryFabricWorldgen.initialize();
        forestry.core.platform.loot.FabricLootIntegration.initialize();
        forestry.core.platform.recipes.RecipeManagers.initialize();
        forestry.core.platform.recipes.FluidContainerIngredient.initialize();
        forestry.core.platform.recipes.ExactComponentIngredient.initialize();
        net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER.entrySet().stream()
            .filter(entry -> java.util.Set.of("minecraft", "forestry").contains(entry.getKey().identifier().getNamespace()))
            .forEach(entry -> net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(entry.getValue()));
        forestry.core.platform.data.ForestryDataMapRuntime.initialize();
        forestry.core.platform.network.ForestryPlatformNetworking.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("FORESTRY_F2_SERVER_READY");
            if (Boolean.getBoolean("forestry.serverAutoExit")) server.halt(false);
        });
    }
}
