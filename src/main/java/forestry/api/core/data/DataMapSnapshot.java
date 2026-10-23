package forestry.api.core.data;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import java.util.*;

/** All map values become visible together, as a single immutable revision. */
public record DataMapSnapshot(long revision, Map<Identifier, Map<Identifier, Object>> values) {
    public static final DataMapSnapshot EMPTY = new DataMapSnapshot(0, Map.of());
    public DataMapSnapshot {
        if (revision < 0) throw new IllegalArgumentException("Negative map revision");
        var copy = new TreeMap<Identifier, Map<Identifier, Object>>();
        values.forEach((key, entries) -> copy.put(key, Collections.unmodifiableMap(new TreeMap<>(entries))));
        values = Collections.unmodifiableMap(copy);
    }
    @SuppressWarnings("unchecked") @Nullable
    public <K, V> V get(DataMapKey<K, V> map, Identifier entry) { return (V) values.getOrDefault(map.id(), Map.of()).get(entry); }
}
