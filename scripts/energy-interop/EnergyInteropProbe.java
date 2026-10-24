package energyinterop;

import com.google.gson.GsonBuilder;
import forestry.core.content.energy.blocks.EngineBlock;
import forestry.core.content.energy.tiles.EngineBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import team.reborn.energy.api.EnergyStorage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnergyInteropProbe implements ModInitializer {
    public interface Scenario {
        String name();
        void sample();
        Map<String, Object> verify();
    }

    private final List<Scenario> scenarios = new ArrayList<>();
    private final Map<String, Object> report = new LinkedHashMap<>();
    private final Path output = Path.of("energy-interop-results.json");
    private int ticks;
    private boolean done;

    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        if (done) return;
        try {
            ticks++;
            if (ticks == 20) {
                report.put("api", inspectApi());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "forceload add 0 0 31 31");
            }
            if (ticks == 60) {
                var level = server.overworld();
                for (int x = 0; x <= 12; x++) for (int z = 0; z <= 31; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                }
                if (Boolean.getBoolean("energyInterop.bc")) scenarios.addAll(BcEnergyScenarios.create(level));
                if (Boolean.getBoolean("energyInterop.tr")) scenarios.addAll(TrEnergyScenarios.create(level));
                require(!scenarios.isEmpty(), "No energy scenarios created");
                System.out.println("ENERGY_INTEROP_FIXTURES_READY " + scenarios.stream().map(Scenario::name).toList());
            }
            if (ticks > 60) scenarios.forEach(Scenario::sample);
            if (ticks == 360) {
                var cases = new LinkedHashMap<String, Object>();
                boolean passed = true;
                for (var scenario : scenarios) {
                    try {
                        cases.put(scenario.name(), Map.of("status", "passed", "measurements", scenario.verify()));
                    } catch (Throwable failure) {
                        passed = false;
                        failure.printStackTrace();
                        cases.put(scenario.name(), Map.of("status", "failed", "error", failure.toString()));
                    }
                }
                report.put("cases", cases);
                report.put("apiAfterTransfers", inspectApi());
                report.put("transferTicks", 300);
                finish(passed, null);
            }
        } catch (Throwable failure) {
            failure.printStackTrace();
            finish(false, failure);
        }
    }

    private Map<String, Object> inspectApi() throws Exception {
        var loader = FabricLoader.getInstance();
        require(!loader.isDevelopmentEnvironment(), "Must use native production mode");
        require(loader.getEnvironmentType() == EnvType.SERVER, "Must run on the dedicated server");
        require(loader.isModLoaded("buildcraftrefabricated") == Boolean.getBoolean("energyInterop.bc"), "Unexpected BC installation");
        require(loader.isModLoaded("techreborn") == Boolean.getBoolean("energyInterop.tr"), "Unexpected TR installation");
        var energyMods = loader.getAllMods().stream().filter(mod -> mod.getMetadata().getId().equals("team_reborn_energy")).toList();
        require(energyMods.size() == 1, "Expected exactly one loaded Energy mod: " + energyMods.size());
        var energy = energyMods.getFirst();
        require(energy.getMetadata().getVersion().getFriendlyString().equals("5.0.0"), "Unexpected Energy API version");
        var resources = Collections.list(EnergyStorage.class.getClassLoader().getResources("team/reborn/energy/api/EnergyStorage.class"))
            .stream().map(Object::toString).distinct().toList();
        require(resources.size() == 1, "Multiple Energy API class definitions: " + resources);
        var origin = energy.getOrigin();
        require(origin.getKind() == ModOrigin.Kind.NESTED, "Energy must come from a nested mod");
        require(Boolean.getBoolean("energyInterop.tr") || origin.getParentModId().equals("forestry"), "BC-only must obtain Energy from Forestry");
        var result = new LinkedHashMap<String, Object>();
        result.put("loadedEnergyMods", energyMods.size());
        result.put("environment", loader.getEnvironmentType().toString());
        result.put("energyVersion", energy.getMetadata().getVersion().getFriendlyString());
        result.put("classResources", resources);
        result.put("codeSource", EnergyStorage.class.getProtectionDomain().getCodeSource().getLocation().toString());
        result.put("originKind", origin.getKind().toString());
        result.put("parentMod", origin.getParentModId());
        result.put("parentSubLocation", origin.getParentSubLocation());
        result.put("containingMod", energy.getContainingMod().map(mod -> mod.getMetadata().getId()).orElse("none"));
        result.put("loadedMods", loader.getAllMods().stream().map(mod -> mod.getMetadata().getId() + " " + mod.getMetadata().getVersion()).sorted().toList());
        return result;
    }

    private void finish(boolean passed, Throwable error) {
        done = true;
        report.put("status", passed ? "passed" : "failed");
        report.put("ticks", ticks);
        if (error != null) report.put("error", error.toString());
        try {
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        }
        System.out.println(passed ? "ENERGY_INTEROP_PASSED" : "ENERGY_INTEROP_FAILED");
    }

    public static BlockEntity place(ServerLevel level, BlockPos pos, String id) {
        var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        require(block != null && block != Blocks.AIR, "Missing block " + id);
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        var entity = level.getBlockEntity(pos);
        require(entity != null, "Missing block entity " + id);
        return entity;
    }

    public static EngineBlockEntity forestryEngine(ServerLevel level, BlockPos pos, Direction facing, long seed) {
        place(level, pos, "forestry:peat_engine");
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(EngineBlock.VERTICAL_FACING, facing));
        var engine = (EngineBlockEntity) level.getBlockEntity(pos);
        engine.getEnergyManager().setEnergyStored(seed);
        level.setBlockAndUpdate(pos.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        return engine;
    }

    public static EnergyStorage storage(ServerLevel level, BlockPos pos, Direction side) {
        var result = EnergyStorage.SIDED.find(level, pos, side);
        require(result != null, "Missing E storage at " + pos + " side " + side);
        return result;
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
