package forestry.fabric.test;

import forestry.Forestry;
import forestry.core.platform.data.LogicalData;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.util.concurrent.CompletableFuture;
import static forestry.fabric.test.F1RegistrationTests.require;

@SuppressWarnings("UnstableApiUsage")
public final class F3WorldgenClientTest implements FabricClientGameTest {
    private static final long SEED = 3479480188525016624L;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> require(SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 0, "Normal world must start before client genetics are synchronized"));
        TestWorldSave save;
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            var preset = settings.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL);
            settings.setWorldType(new WorldCreationUiState.WorldTypeEntry(preset));
            settings.setSeed(Long.toString(SEED));
            settings.setGenerateStructures(true);
            settings.setName("Forestry normal worldgen regression");
        }).create()) {
            save = world.getWorldSave();
            world.getServer().runOnServer(server -> {
                require(server.overworld().getSeed() == SEED, "Incorrect normal-world regression seed");
                require(server.overworld().getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator, "Worldgen regression accidentally used a flat world");
                server.overworld().getChunk(64, 64);
            });
            context.waitFor(client -> client.player != null && SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 69);
            verifyWorkerIsolation(context, world);
            Forestry.LOGGER.info("FORESTRY_NORMAL_WORLDGEN_CREATED seed={}", SEED);
        }
        context.waitFor(client -> client.level == null && SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 0);
        try (var world = save.open()) {
            world.getServer().runOnServer(server -> server.overworld().getChunk(96, 96));
            context.waitFor(client -> client.player != null && SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 69);
            verifyWorkerIsolation(context, world);
            Forestry.LOGGER.info("FORESTRY_NORMAL_WORLDGEN_REOPENED seed={}", SEED);
        }
        context.waitFor(client -> client.level == null && SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 0);
    }

    private static void verifyWorkerIsolation(ClientGameTestContext context, TestSingleplayerContext world) {
        var marker = new LogicalData<>("initial");
        context.runOnClient(client -> marker.set("client"));
        var serverSpecies = world.getServer().computeOnServer(server -> {
            marker.set("server");
            return SpeciesUtil.getBeeSpecies(forestry.api.apiculture.ForestryBeeSpecies.MODEST);
        });
        context.runOnClient(client -> require(SpeciesUtil.getBeeSpecies(serverSpecies.id()) != serverSpecies, "Client and server unexpectedly share genetic objects"));
        var worker = CompletableFuture.runAsync(() -> {
            require(!LogicalData.isServer() && marker.get().equals("client"), "Unscoped worker unexpectedly inherited server data");
            LogicalData.withServerSnapshot(() -> {
                require(LogicalData.isServer() && marker.get().equals("server"), "Worldgen worker did not select server data");
                require(SpeciesUtil.getBeeSpecies(serverSpecies.id()) == serverSpecies, "Worldgen worker read the client's species projection");
                require(SpeciesUtil.TREE_TYPE.get().getSpeciesCount() == 50, "Worldgen worker lost tree species");
                var failure = new IllegalStateException("nested scope regression");
                try {
                    LogicalData.withServerSnapshot(() -> { throw failure; });
                    throw new AssertionError("Nested scope swallowed an exception");
                } catch (IllegalStateException expected) {
                    require(expected == failure && marker.get().equals("server"), "Nested scope corrupted its parent");
                }
                return null;
            });
            require(!LogicalData.isServer() && marker.get().equals("client"), "Server snapshot leaked onto a pooled worker");
            var failure = new IllegalStateException("outer scope regression");
            try {
                LogicalData.withServerSnapshot(() -> { throw failure; });
                throw new AssertionError("Server scope swallowed an exception");
            } catch (IllegalStateException expected) {
                require(expected == failure && !LogicalData.isServer() && marker.get().equals("client"), "Failed worldgen leaked its server context");
            }
        });
        context.waitFor(client -> worker.isDone());
        worker.join();
        Forestry.LOGGER.info("FORESTRY_WORLDGEN_WORKER_ISOLATION_VERIFIED");
    }
}
