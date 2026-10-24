import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.discovery.DirectoryModCandidateFinder;
import net.fabricmc.loader.impl.discovery.ModCandidateImpl;
import net.fabricmc.loader.impl.discovery.ModDiscoverer;
import net.fabricmc.loader.impl.discovery.ModResolutionException;
import net.fabricmc.loader.impl.discovery.ModResolver;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.DependencyOverrides;
import net.fabricmc.loader.impl.metadata.VersionOverrides;

public final class EnergyBundlingProbe {
    private static final String ENERGY_ID = "team_reborn_energy";
    private static EnvType environment;

    public static void main(String[] args) throws Exception {
        if (args.length < 6) throw new IllegalArgumentException("core.jar fabric-api.jar output-dir minecraft-version loader-version energy-version [companion.jar ...]");
        Path core = Path.of(args[0]).toAbsolutePath();
        Path fabricApi = Path.of(args[1]).toAbsolutePath();
        Path output = Files.createTempDirectory(Files.createDirectories(Path.of(args[2])), "resolution-");
        System.setProperty("fabric.log.file", output.resolve("fabricloader.log").toString());
        String minecraftVersion = args[3];
        String loaderVersion = args[4];
        String energyVersion = args[5];
        var companions = new ArrayList<Path>();
        for (int index = 6; index < args.length; index++) companions.add(Path.of(args[index]).toAbsolutePath());
        byte[] energy;
        try (var zip = new ZipFile(core.toFile())) {
            ZipEntry entry = zip.getEntry("META-INF/jars/energy-" + energyVersion + ".jar");
            if (entry == null) throw new AssertionError("Release core does not bundle Energy API");
            energy = zip.getInputStream(entry).readAllBytes();
        }

        FabricLauncher launcher = (FabricLauncher) Proxy.newProxyInstance(EnergyBundlingProbe.class.getClassLoader(),
            new Class<?>[]{FabricLauncher.class}, (proxy, method, values) -> switch (method.getName()) {
                case "getEnvironmentType" -> environment;
                case "isDevelopment" -> false;
                default -> throw new UnsupportedOperationException(method.toString());
            });
        FabricLauncherBase.setLauncher(launcher);
        List<GameProvider.BuiltinMod> builtins = List.of(
            new GameProvider.BuiltinMod(List.of(core), new BuiltinModMetadata.Builder("minecraft", minecraftVersion).build()),
            new GameProvider.BuiltinMod(List.of(core), new BuiltinModMetadata.Builder("fabricloader", loaderVersion).build()));
        GameProvider provider = (GameProvider) Proxy.newProxyInstance(EnergyBundlingProbe.class.getClassLoader(),
            new Class<?>[]{GameProvider.class}, (proxy, method, values) -> switch (method.getName()) {
                case "getLaunchDirectory" -> output;
                case "getBuiltinMods" -> builtins;
                default -> throw new UnsupportedOperationException(method.toString());
            });
        FabricLoaderImpl.INSTANCE.setGameProvider(provider);

        var results = new ArrayList<String>();
        for (Path artifact : List.of(core, fabricApi)) recordArtifact(artifact, results);
        for (Path artifact : companions) recordArtifact(artifact, results);
        for (EnvType side : EnvType.values()) {
            environment = side;
            runCase(output, core, fabricApi, energy, energyVersion, "core-only", null, null, energyVersion, false, List.of(), results);
            runCase(output, core, fabricApi, energy, energyVersion, "consumer-without-provider", null, "^" + energyVersion, energyVersion, false, List.of(), results);
            runCase(output, core, fabricApi, energy, energyVersion, "duplicate-nested", energyVersion, "^" + energyVersion, energyVersion, false, List.of(), results);
            runCase(output, core, fabricApi, energy, energyVersion, "duplicate-external", "external", "^" + energyVersion, energyVersion, false, List.of(), results);
            // synthetic metadata tests the version constraint, not a future API binary
            runCase(output, core, fabricApi, energy, energyVersion, "newer-compatible-nested", "5.1.0", ">=5.1.0 <6", "5.1.0", false, List.of(), results);
            runCase(output, core, fabricApi, energy, energyVersion, "older-nested", "4.1.0", null, energyVersion, false, List.of(), results);
            runCase(output, core, fabricApi, energy, energyVersion, "incompatible-major", "6.0.0", ">=6", null, true, List.of(), results);
            for (int index = 0; index < companions.size(); index++) {
                runCase(output, core, fabricApi, energy, energyVersion, "provided-" + index, null, null, energyVersion, false, List.of(companions.get(index)), results);
            }
            if (companions.size() > 1) {
                runCase(output, core, fabricApi, energy, energyVersion, "provided-together", null, null, energyVersion, false, companions, results);
            }
        }
        Files.writeString(output.resolve("results.txt"), String.join("\n", results) + "\n", StandardCharsets.UTF_8);
        System.out.println("Energy bundling: " + (results.size() - 2 - companions.size()) + " cases passed; " + output);
    }

    private static void runCase(Path output, Path core, Path fabricApi, byte[] energy, String energyVersion,
                                String name, String companionVersion, String consumerRange, String expected,
                                boolean rejected, List<Path> companions, List<String> results) throws Exception {
        Path directory = Files.createDirectories(output.resolve(environment.name().toLowerCase() + "-" + name));
        Files.copy(core, directory.resolve(core.getFileName()));
        Files.copy(fabricApi, directory.resolve(fabricApi.getFileName()));
        for (Path companion : companions) Files.copy(companion, directory.resolve(companion.getFileName()));
        if (consumerRange != null) {
            Files.write(directory.resolve("consumer.jar"), fixture("energy_consumer_fixture", "1.0.0", consumerRange, null));
        }
        if ("external".equals(companionVersion)) {
            Files.write(directory.resolve("energy.jar"), energy);
        } else if (companionVersion != null) {
            byte[] nested = companionVersion.equals(energyVersion) ? energy : fixture(ENERGY_ID, companionVersion, null, null);
            Files.write(directory.resolve("carrier.jar"), fixture("energy_carrier_fixture", "1.0.0", null, nested));
        }

        var discoverer = new ModDiscoverer(new VersionOverrides(), new DependencyOverrides(directory.resolve("config")));
        discoverer.addCandidateFinder(new DirectoryModCandidateFinder(directory, false));
        Map<String, Set<ModCandidateImpl>> disabled = new HashMap<>();
        var candidates = discoverer.discoverMods(FabricLoaderImpl.INSTANCE, disabled);
        List<ModCandidateImpl> resolved;
        try {
            resolved = ModResolver.resolve(candidates, environment, disabled);
        } catch (ModResolutionException exception) {
            if (!rejected) throw exception;
            String result = environment + " " + name + " rejected as expected";
            results.add(result);
            System.out.println(result);
            return;
        }
        if (rejected) throw new AssertionError("Incompatible energy major unexpectedly resolved");
        var selected = resolved.stream().filter(mod -> mod.getId().equals(ENERGY_ID)).toList();
        if (selected.size() != 1 || !selected.getFirst().getVersion().getFriendlyString().equals(expected)) {
            throw new AssertionError(name + ": expected exactly one Energy " + expected + ", got " + selected);
        }
        if (resolved.stream().noneMatch(mod -> mod.getId().equals("forestry"))) throw new AssertionError("Forestry missing");
        if (companions.isEmpty() && resolved.stream().anyMatch(mod -> mod.getId().equals("techreborn"))) throw new AssertionError("Tech Reborn present");
        for (Path companion : companions) {
            if (resolved.stream().noneMatch(mod -> mod.getOriginPaths() != null
                && mod.getOriginPaths().contains(directory.resolve(companion.getFileName())))) {
                throw new AssertionError("Companion missing: " + companion);
            }
        }
        if (consumerRange != null && resolved.stream().noneMatch(mod -> mod.getId().equals("energy_consumer_fixture"))) {
            throw new AssertionError("Consumer missing");
        }
        String result = environment + " " + name + " selected " + selected.getFirst();
        results.add(result);
        System.out.println(result);
    }

    private static void recordArtifact(Path artifact, List<String> results) throws Exception {
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact)));
        results.add("SHA-256 " + sha256 + " " + artifact);
    }

    private static byte[] fixture(String id, String version, String requiresEnergy, byte[] nested) throws Exception {
        String metadata = "{\"schemaVersion\":1,\"id\":\"" + id + "\",\"version\":\"" + version + "\""
            + (requiresEnergy == null ? "" : ",\"depends\":{\"team_reborn_energy\":\"" + requiresEnergy + "\"}")
            + (nested == null ? "" : ",\"jars\":[{\"file\":\"META-INF/jars/energy.jar\"}]") + "}";
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("fabric.mod.json"));
            zip.write(metadata.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            if (nested != null) {
                zip.putNextEntry(new ZipEntry("META-INF/jars/energy.jar"));
                zip.write(nested);
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }
}
