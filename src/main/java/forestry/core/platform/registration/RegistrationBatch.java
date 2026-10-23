package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Small registration boundary: no event bus, loader scanning, or runtime registration after initialization. */
public final class RegistrationBatch<R> {
    private static final List<RegistrationBatch<?>> BATCHES = new ArrayList<>();
    private static boolean finished;
    private final ResourceKey<? extends Registry<R>> registryKey;
    private final String namespace;
    private final Map<Identifier, RegistryEntry<R, ?>> entries = new LinkedHashMap<>();

    private RegistrationBatch(ResourceKey<? extends Registry<R>> key, String namespace) {
        if (finished) throw new IllegalStateException("Forestry registration is already complete");
        this.registryKey = key;
        this.namespace = namespace;
        BATCHES.add(this);
    }

    public static <R> RegistrationBatch<R> create(ResourceKey<? extends Registry<R>> key, String namespace) {
        return new RegistrationBatch<>(key, namespace);
    }

    public static <R> RegistrationBatch<R> create(Registry<R> registry, String namespace) {
        return create(registry.key(), namespace);
    }

    @SuppressWarnings("unchecked")
    private Registry<R> registry() {
        Registry<R> result = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(this.registryKey.identifier());
        if (result == null) throw new IllegalStateException("Registry must be created before binding: " + this.registryKey);
        return result;
    }

    public <T extends R> RegistryEntry<R, T> register(String path, Supplier<T> factory) {
        if (finished) throw new IllegalStateException("Late Forestry registration: " + path);
        Identifier id = Identifier.fromNamespaceAndPath(this.namespace, path);
        RegistryEntry<R, T> entry = new RegistryEntry<>(ResourceKey.create(this.registryKey, id), this::registry, factory);
        if (this.entries.putIfAbsent(id, entry) != null) throw new IllegalArgumentException("Duplicate registration declaration: " + id);
        return entry;
    }

    public Collection<RegistryEntry<R, ?>> getEntries() { return List.copyOf(this.entries.values()); }
    public ResourceKey<? extends Registry<R>> getRegistryKey() { return this.registryKey; }

    public static void bindAll() {
        if (finished) throw new IllegalStateException("Forestry registries were bound twice");
        for (int i = 0; i < BATCHES.size(); i++) {
            for (RegistryEntry<?, ?> entry : BATCHES.get(i).entries.values()) entry.get();
        }
        finished = true;
        ModFeatureRegistry.getRegistries().values().forEach(FeatureRegistry::notifyBound);
    }
}
