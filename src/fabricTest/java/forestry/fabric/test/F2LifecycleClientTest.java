package forestry.fabric.test;

import forestry.Forestry;
import forestry.api.ForestryDataMaps;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.core.platform.data.ForestryDataMapRuntime;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static forestry.fabric.test.F1RegistrationTests.require;

@SuppressWarnings("UnstableApiUsage")
public final class F2LifecycleClientTest implements FabricClientGameTest {
    private static final Identifier APPLE = Identifier.parse("minecraft:apple");
    private final AtomicInteger stoppingSpecies = new AtomicInteger();
    private final AtomicInteger stoppingTrees = new AtomicInteger();
    @Override public void runTest(ClientGameTestContext context) {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            stoppingSpecies.set(SpeciesUtil.BEE_TYPE.get().getSpeciesCount());
            stoppingTrees.set(SpeciesUtil.TREE_TYPE.get().getSpeciesCount());
        });
        TestWorldSave serverA;
        try (var world = context.worldBuilder().create()) {
            serverA = world.getWorldSave();
            context.runOnClient(F1ClientTest::verifyCreativeSpecies);
            var originalOakGenome = context.computeOnClient(client -> SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).getDefaultGenome().getAllelePairs());
            writeConfig(serverA, 11);
            loadConfig(world);
            writePack(serverA, 11);
            reload(context, world, true);
            assertJoined(context, world, 11);
            removeTestSpecies(serverA);
            reload(context, world, true);
            context.runOnClient(client -> {
                require(SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 69, "Removed datapack species remains registered");
                require(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).getDefaultGenome().getAllelePairs().equals(originalOakGenome),
                    "Removed datapack tree genome override remains registered");
                F1ClientTest.verifyCreativeSpecies(client);
                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) F1JeiProbe.verifyGeneticIngredients();
            });
            writePack(serverA, 11);
            reload(context, world, true);
            assertJoined(context, world, 11);
            long revision = context.computeOnClient(client -> ForestryDataMapRuntime.clientSnapshot().revision());
            var previous = world.getServer().computeOnServer(ForestryDataMapRuntime::snapshot);
            writePack(serverA, -1);
            reload(context, world, false);
            require(world.getServer().computeOnServer(ForestryDataMapRuntime::snapshot) == previous, "Failed reload published staged DataMaps");
            context.runOnClient(client -> require(ForestryDataMapRuntime.clientSnapshot().revision() == revision, "Failed reload sent client data"));
            writePack(serverA, 11);
            reload(context, world, true);
        }
        assertDisconnected(context);
        try (var world = serverA.open()) { assertJoined(context, world, 11); }
        assertDisconnected(context);
        try (var world = context.worldBuilder().create()) {
            writeConfig(world.getWorldSave(), 22);
            loadConfig(world);
            writePack(world.getWorldSave(), 22);
            reload(context, world, true);
            assertJoined(context, world, 22);
        }
        assertDisconnected(context);
        try (var world = serverA.open()) { assertJoined(context, world, 11); }
        assertDisconnected(context);
        Forestry.LOGGER.info("FORESTRY_F2_RELOAD_RECONNECT_PASSED A-A-B-A");
    }
    private void assertDisconnected(ClientGameTestContext context) {
        try { context.waitFor(client -> client.level == null && ForestryDataMapRuntime.clientSnapshot().revision() == 0 && SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 0 && SpeciesUtil.TREE_TYPE.get().getSpeciesCount() == 0); }
        catch (AssertionError error) { throw new AssertionError(context.computeOnClient(client -> "Disconnect state: level=" + client.level + ", listener=" + client.getConnection() + ", maps=" + ForestryDataMapRuntime.clientSnapshot().revision() + ", bees=" + SpeciesUtil.BEE_TYPE.get().getSpeciesCount() + ", trees=" + SpeciesUtil.TREE_TYPE.get().getSpeciesCount() + ", " + forestry.core.platform.client.ForestryClientNetworking.connectionDiagnostics()), error); }
        require(stoppingSpecies.get() == 70, "Client disconnect cleared integrated server genetics before shutdown");
        require(stoppingTrees.get() == 50, "Client disconnect cleared integrated server tree genetics before shutdown");
        context.runOnClient(client -> require(forestry.api.IForestryApi.INSTANCE.getClimateManager().getTemperature(net.minecraft.world.level.biome.Biomes.JUNGLE) == forestry.api.core.TemperatureType.NORMAL, "Disconnected climate cache was retained"));
    }
    private static void assertJoined(ClientGameTestContext context, TestSingleplayerContext world, int fertilizer) {
        context.waitFor(client -> client.player != null && forestry.core.platform.config.ForestryConfig.SERVER.solarFE.get() == fertilizer && Objects.equals(ForestryDataMapRuntime.clientSnapshot().get(ForestryDataMaps.FARM_FERTILIZERS, APPLE), fertilizer));
        context.runOnClient(client -> {
            require(SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 70, "Client genome definitions missing");
            require(SpeciesUtil.TREE_TYPE.get().getSpeciesCount() == 50, "Client tree definitions missing");
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("forestrybutterflies")) require(SpeciesUtil.BUTTERFLY_TYPE.get().getSpeciesCount() == 35, "Client butterfly definitions missing");
            require(forestry.core.platform.util.RecipeUtils.getRecipe(Identifier.parse("minecraft:oak_planks")) != null, "Worktable vanilla recipes were not synchronized");
            require(!SpeciesUtil.BEE_TYPE.get().getMutations().getMutationsFrom(SpeciesUtil.BEE_TYPE.get().getDefaultSpecies()).isEmpty(), "Client mutation data missing");
            F1ClientTest.verifyCreativeSpecies(client);
            assertClimate(client.level.registryAccess(), fertilizer);
        });
        world.getServer().runOnServer(server -> assertClimate(server.registryAccess(), fertilizer));
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
            context.waitFor(client -> F1JeiProbe.isReady());
            context.runOnClient(client -> F1JeiProbe.verifyGeneticIngredients());
        }
        var serverSpecies = world.getServer().computeOnServer(server -> SpeciesUtil.BEE_TYPE.get().getDefaultSpecies());
        require(context.computeOnClient(client -> SpeciesUtil.BEE_TYPE.get().getDefaultSpecies()) != serverSpecies, "Client and server share mutable projected domain objects");
    }
    private static void assertClimate(net.minecraft.core.RegistryAccess registries, int value) {
        var key = net.minecraft.world.level.biome.Biomes.JUNGLE;
        var biome = registries.lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(key);
        var climates = forestry.api.IForestryApi.INSTANCE.getClimateManager();
        var temperature = value == 22 ? forestry.api.core.TemperatureType.COLD : forestry.api.core.TemperatureType.WARM;
        var humidity = value == 22 ? forestry.api.core.HumidityType.ARID : forestry.api.core.HumidityType.DAMP;
        require(climates.getTemperature(biome) == temperature && climates.getTemperature(key) == temperature, "Biome temperature did not follow the current world's tags");
        require(climates.getHumidity(biome) == humidity && climates.getHumidity(key) == humidity, "Biome humidity did not follow the current world's tags");
        require(SpeciesUtil.getBeeSpecies(Identifier.parse("forestry:lifecycle_test_bee")).getDefaultGenome().getActiveValue(forestry.api.core.genetics.alleles.BeeChromosomes.FERTILITY) == (value == 22 ? 4 : 3), "Creative species definition leaked between worlds");
        require(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).getDefaultGenome().getActiveValue(TreeChromosomes.MATURATION) == (value == 22 ? 4 : 3),
            "Creative tree species definition leaked between worlds");
    }
    private static void writeConfig(TestWorldSave save, int value) {
        try {
            var file = save.getSaveDirectory().resolve("serverconfig/forestry-server.json");
            Files.createDirectories(file.getParent()); Files.writeString(file, "{\"solar_engine.solar_fe_per_panel\":" + value + "}");
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    private static void removeTestSpecies(TestWorldSave save) {
        try {
            Files.delete(save.getSaveDirectory().resolve("datapacks/forestry_f2_lifecycle/data/forestry/bee_species/lifecycle_test_bee.json"));
            Files.delete(save.getSaveDirectory().resolve("datapacks/forestry_f2_lifecycle/data/forestry/tree_species/oak.json"));
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    private static void loadConfig(TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> {
            try { forestry.core.platform.config.ForestryConfig.SERVER.solarFE.set(com.google.gson.JsonParser.parseString(Files.readString(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("serverconfig/forestry-server.json"))).getAsJsonObject().get("solar_engine.solar_fe_per_panel").getAsDouble()); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        });
    }
    private static void writePack(TestWorldSave save, int value) {
        try {
            var root = save.getSaveDirectory().resolve("datapacks/forestry_f2_lifecycle");
            var file = root.resolve("data/forestry/data_maps/item/farm_fertilizers.json");
            Files.createDirectories(file.getParent());
            var format = net.minecraft.SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA);
            String range = "[" + format.major() + "," + format.minor() + "]";
            Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Forestry F2 regression\",\"min_format\":" + range + ",\"max_format\":" + range + "}}");
            Files.writeString(file, "{\"values\":{\"minecraft:apple\":" + value + "}}");
            var species = root.resolve("data/forestry/bee_species/lifecycle_test_bee.json");
            Files.createDirectories(species.getParent());
            Files.writeString(species, "{\"genus\":\"apis\",\"species\":\"lifecycle\",\"genome\":{\"forestry:fertility\":{\"value\":" + (value == 22 ? 4 : 3) + "}}}");
            var tree = root.resolve("data/forestry/tree_species/oak.json");
            Files.createDirectories(tree.getParent());
            Files.writeString(tree, "{\"genus\":\"quercus\",\"species\":\"robur\",\"genome\":{\"forestry:maturation\":{\"value\":" + (value == 22 ? 4 : 3) + "}}}");
            for (String tag : List.of("temperature/cold", "humidity/arid")) {
                var climate = root.resolve("data/forestry/tags/worldgen/biome/" + tag + ".json");
                Files.createDirectories(climate.getParent());
                Files.writeString(climate, "{\"values\":[" + (value == 22 ? "\"minecraft:jungle\"" : "") + "]}");
            }
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    private static void reload(ClientGameTestContext context, TestSingleplayerContext world, boolean success) {
        CompletableFuture<Void> reload = world.getServer().computeOnServer(server -> {
            server.getPackRepository().reload();
            var selected = new LinkedHashSet<>(server.getPackRepository().getSelectedIds()); selected.add("file/forestry_f2_lifecycle");
            return server.reloadResources(selected);
        });
        context.waitFor(client -> reload.isDone());
        require(reload.isCompletedExceptionally() != success, "Unexpected datapack reload outcome");
        if (success) reload.join();
        world.getConnection().waitForClientboundPackets();
    }
}
