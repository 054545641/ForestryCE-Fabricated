package forestry.core.platform.config;

import com.google.gson.*;
import org.apache.commons.lang3.tuple.Pair;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** Typed Forestry configuration with atomic validation and a separate, temporary server override. */
public final class ConfigSpec {
    private final Map<String, Value<?>> definitions;
    private final AtomicReference<Map<String, Object>> local;
    private volatile Map<String, Object> remote;
    private ConfigSpec(Map<String, Value<?>> definitions) {
        this.definitions = Map.copyOf(definitions);
        var defaults = new LinkedHashMap<String, Object>();
        definitions.forEach((key, value) -> { value.owner = this; defaults.put(key, value.fallback); });
        this.local = new AtomicReference<>(Map.copyOf(defaults));
    }
    public static abstract class Value<T> {
        private ConfigSpec owner;
        private final String key;
        private final T fallback;
        private Value(String key, T fallback) { this.key = key; this.fallback = fallback; }
        @SuppressWarnings("unchecked") public T get() {
            var current = forestry.core.platform.data.LogicalData.isServer() ? null : owner.remote;
            return (T) (current == null ? owner.local.get() : current).get(key);
        }
        public void set(T value) {
            T checked = decode(new Gson().toJsonTree(value));
            owner.local.updateAndGet(before -> { var next = new LinkedHashMap<>(before); next.put(key, checked); return Map.copyOf(next); });
        }
        protected abstract T decode(JsonElement json);
    }
    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(String key, boolean fallback) { super(key, fallback); }
        @Override protected Boolean decode(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Expected a boolean config value");
            return json.getAsBoolean();
        }
    }
    public static final class IntValue extends Value<Integer> {
        private final int min, max;
        private IntValue(String key, int fallback, int min, int max) { super(key, fallback); this.min = min; this.max = max; }
        @Override protected Integer decode(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected an integer config value");
            int value = json.getAsBigDecimal().intValueExact();
            if (value < min || value > max) throw new IllegalArgumentException("Config value outside " + min + ".." + max);
            return value;
        }
    }
    public static final class DoubleValue extends Value<Double> {
        private final double min, max;
        private DoubleValue(String key, double fallback, double min, double max) { super(key, fallback); this.min = min; this.max = max; }
        @Override protected Double decode(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected a numeric config value");
            double value = json.getAsDouble();
            if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Config value outside " + min + ".." + max);
            return value;
        }
    }
    public static final class Builder {
        private final Map<String, Value<?>> definitions = new LinkedHashMap<>();
        private final Deque<String> path = new ArrayDeque<>();
        public Builder push(String key) { path.addLast(key); return this; }
        public Builder pop() { path.removeLast(); return this; }
        public Builder comment(String... description) { return this; }
        private String key(String key) { return path.isEmpty() ? key : String.join(".", path) + "." + key; }
        private <T extends Value<?>> T add(String key, T value) {
            if (definitions.putIfAbsent(key(key), value) != null) throw new IllegalArgumentException("Duplicate config key: " + key);
            return value;
        }
        public BooleanValue define(String key, boolean fallback) { return add(key, new BooleanValue(key(key), fallback)); }
        public IntValue defineInRange(String key, int fallback, int min, int max) { return add(key, new IntValue(key(key), fallback, min, max)); }
        public DoubleValue defineInRange(String key, double fallback, double min, double max) { return add(key, new DoubleValue(key(key), fallback, min, max)); }
        public <T> Pair<T, ConfigSpec> configure(Function<Builder, T> factory) {
            T result = factory.apply(this);
            if (!path.isEmpty()) throw new IllegalStateException("Unclosed config section");
            return Pair.of(result, new ConfigSpec(definitions));
        }
    }
    public Map<String, Object> validate(JsonObject json, boolean requireAll) {
        if (requireAll && json.size() != definitions.size()) throw new IllegalArgumentException("Incomplete server config snapshot");
        var result = new LinkedHashMap<String, Object>();
        definitions.forEach((key, definition) -> result.put(key, definition.fallback));
        for (var entry : json.entrySet()) {
            var definition = definitions.get(entry.getKey());
            if (definition == null) throw new IllegalArgumentException("Unknown config key: " + entry.getKey());
            result.put(entry.getKey(), definition.decode(entry.getValue()));
        }
        return Map.copyOf(result);
    }
    public void load(Path file) throws IOException {
        if (Files.exists(file)) {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            local.set(validate(json, false));
        } else {
            Files.createDirectories(file.getParent());
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(snapshot()) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
    }
    /** Forestry's legacy generated TOML contains only scalar numeric/boolean keys and tables. */
    public JsonObject convertLegacyToml(String source) {
        JsonObject converted = new JsonObject();
        String section = "";
        for (String raw : source.split("\\R")) {
            String line = raw.replaceFirst("#.*$", "").trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).replace("\"", "").trim();
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 1) throw new IllegalArgumentException("Unsupported Forestry TOML line: " + raw);
            String key = line.substring(0, equals).replace("\"", "").trim();
            if (!section.isEmpty()) key = section + "." + key;
            if (converted.has(key)) throw new IllegalArgumentException("Duplicate legacy configuration key: " + key);
            String value = line.substring(equals + 1).trim().replace("_", "");
            if (!value.equals("true") && !value.equals("false") && !value.matches("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?"))
                throw new IllegalArgumentException("Unsupported legacy configuration value for " + key);
            converted.add(key, JsonParser.parseString(value));
        }
        return new Gson().toJsonTree(validate(converted, false)).getAsJsonObject();
    }
    public void load(Path json, Path legacyToml) throws IOException {
        if (!Files.exists(json) && Files.exists(legacyToml)) {
            JsonObject candidate = convertLegacyToml(Files.readString(legacyToml, StandardCharsets.UTF_8));
            Files.createDirectories(json.getParent());
            Files.writeString(json, new GsonBuilder().setPrettyPrinting().create().toJson(candidate) + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        load(json);
    }
    public JsonObject snapshot() { return new Gson().toJsonTree(local.get()).getAsJsonObject(); }
    public void applyRemote(JsonObject values) { remote = validate(values, true); }
    public void clearRemote() { remote = null; }
}
