package forestry.datagen;

import com.google.common.hash.Hashing;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ForestryFabricDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider((FabricDataGenerator.Pack.Factory<DataProvider>) output -> new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                return CompletableFuture.runAsync(() -> {
                    Path root = Path.of(System.getProperty("forestry.datagen.root"));
                    Path staging = root.resolve("build/fabric-26.2/datagen-staging");
                    ClassLoader previous = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(ForestryResourceGenerator.class.getClassLoader());
                        new ForestryResourceGenerator(root).generate(staging);
                    } finally {
                        Thread.currentThread().setContextClassLoader(previous);
                    }
                    try (var files = Files.walk(staging)) {
                        for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                            byte[] bytes = Files.readAllBytes(file);
                            cache.writeIfNeeded(output.getOutputFolder().resolve(staging.relativize(file)), bytes, Hashing.sha1().hashBytes(bytes));
                        }
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
            }

            @Override
            public String getName() {
                return "Forestry native Fabric resources (four modules)";
            }
        });
    }
}
