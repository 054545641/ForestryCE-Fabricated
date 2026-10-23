package forestry.core.platform.data;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import forestry.api.ForestryDataMaps;
import forestry.api.core.data.DataMapKey;
import forestry.api.core.data.DataMapSnapshot;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.tags.TagKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Publishes into Fabric's reload-scoped store, so failed Minecraft reloads keep the old complete snapshot. */
public final class DataMapLoader implements PreparableReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("forestry", "data_maps");
    public static final DataResourceStore.Key<DataMapSnapshot> KEY = new DataResourceStore.Key<>();
    public static final int MAX_DOCUMENT_BYTES = 1_048_576;
    private static final AtomicLong REVISIONS = new AtomicLong();
    public record Document(String source, JsonObject json) {}

    @Override public CompletableFuture<Void> reload(SharedState state, Executor prepare, PreparationBarrier barrier, Executor apply) {
        return CompletableFuture.supplyAsync(() -> {
            Map<DataMapKey<?, ?>, List<Document>> documents = new LinkedHashMap<>();
            for (var map : ForestryDataMaps.all()) {
                var file = Identifier.fromNamespaceAndPath(map.id().getNamespace(), "data_maps/" + map.registry().identifier().getPath() + "/" + map.id().getPath() + ".json");
                var layers = new ArrayList<Document>();
                // Resource stacks are in ascending pack priority. Each higher pack applies after the lower one.
                for (var resource : state.resourceManager().getResourceStack(file)) {
                    try (var stream = resource.open()) {
                        byte[] bytes = stream.readNBytes(MAX_DOCUMENT_BYTES + 1);
                        if (bytes.length > MAX_DOCUMENT_BYTES) throw new IllegalArgumentException("DataMap document is too large: " + file);
                        layers.add(new Document(resource.sourcePackId() + ":" + file,
                            JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject()));
                    } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
                }
                documents.put(map, List.copyOf(layers));
            }
            return documents;
        }, prepare).thenCompose(barrier::wait).thenAcceptAsync(documents -> {
            var lookup = state.get(ResourceLoader.REGISTRY_LOOKUP_KEY);
            var snapshot = parse(documents, lookup, REVISIONS.incrementAndGet());
            state.get(DataResourceLoader.DATA_RESOURCE_STORE_KEY).put(KEY, snapshot);
        }, apply);
    }

    public static DataMapSnapshot parse(Map<DataMapKey<?, ?>, List<Document>> documents, HolderLookup.Provider lookup, long revision) {
        var result = new TreeMap<Identifier, Map<Identifier, Object>>();
        documents.forEach((map, layers) -> result.put(map.id(), parseMap(map, layers, lookup)));
        return new DataMapSnapshot(revision, result);
    }

    private static <K, V> Map<Identifier, Object> parseMap(DataMapKey<K, V> map, List<Document> layers, HolderLookup.Provider lookup) {
        var entries = new TreeMap<Identifier, Object>();
        var registry = lookup.lookupOrThrow(map.registry());
        var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
        for (var document : layers) {
            try {
                JsonObject json = document.json();
                for (String key : json.keySet()) {
                    if (!Set.of("schema", "replace", "values", "remove").contains(key)) throw new IllegalArgumentException("Unsupported DataMap field: " + key);
                }
                int schema = json.has("schema") ? json.get("schema").getAsBigDecimal().intValueExact() : 1;
                if (schema != 1 && schema != 2) throw new IllegalArgumentException("Unsupported DataMap schema: " + schema);
                if (booleanField(json, "replace", false)) entries.clear();
                var values = json.has("values") ? json.getAsJsonObject("values") : new JsonObject();
                // Same-pack literal entries beat tag defaults regardless of JSON object order.
                var ordered = new ArrayList<>(values.entrySet());
                ordered.sort(Comparator.<Map.Entry<String, JsonElement>, Boolean>comparing(entry -> !entry.getKey().startsWith("#"))
                    .thenComparing(Map.Entry::getKey));
                for (var entry : ordered) {
                    JsonElement raw = entry.getValue();
                    boolean required = true;
                    if (raw.isJsonObject() && raw.getAsJsonObject().has("value")) {
                        JsonObject wrapper = raw.getAsJsonObject();
                        required = booleanField(wrapper, "required", true);
                        booleanField(wrapper, "replace", false); // Flat records use last-value-wins; no deep field merging.
                        raw = wrapper.get("value");
                    }
                    V value = map.codec().parse(ops, raw).getOrThrow();
                    for (Identifier target : targets(map, registry, entry.getKey(), required)) entries.put(target, value);
                }
                if (json.has("remove")) {
                    for (JsonElement removal : json.getAsJsonArray("remove")) {
                        String target = removal.isJsonObject() ? removal.getAsJsonObject().get("id").getAsString() : removal.getAsString();
                        boolean required = removal.isJsonObject() && booleanField(removal.getAsJsonObject(), "required", false);
                        for (Identifier id : targets(map, registry, target, required)) entries.remove(id);
                    }
                }
                if (entries.size() > 16_384) throw new IllegalArgumentException("Too many DataMap entries");
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("Invalid " + map.id() + " in " + document.source() + ": " + error.getMessage(), error);
            }
        }
        return entries;
    }

    private static boolean booleanField(JsonObject json, String key, boolean fallback) {
        if (!json.has(key)) return fallback;
        JsonElement value = json.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected boolean: " + key);
        return value.getAsBoolean();
    }

    private static <K, V> List<Identifier> targets(DataMapKey<K, V> map, HolderLookup.RegistryLookup<K> lookup, String selector, boolean required) {
        boolean tag = selector.startsWith("#");
        Identifier id = Identifier.parse(tag ? selector.substring(1) : selector);
        if (tag) {
            var values = lookup.get(TagKey.create(map.registry(), id));
            if (values.isPresent() && values.get().isBound()) return values.get().stream().map(holder -> holder.unwrapKey().orElseThrow().identifier()).toList();
        } else if (lookup.get(ResourceKey.create(map.registry(), id)).isPresent()) return List.of(id);
        if (required) throw new IllegalArgumentException("Unknown required selector: " + selector);
        return List.of();
    }
}
