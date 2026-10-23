package forestry.core.platform.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import forestry.api.ForestryDataMaps;
import forestry.api.core.data.DataMapKey;
import forestry.api.core.data.DataMapSnapshot;
import net.minecraft.core.HolderLookup;
import java.util.*;

/** Only the server's resolved values cross the wire; clients never merge local packs into this state. */
public final class DataMapSync {
    private DataMapSync() {}
    public static JsonObject encode(DataMapSnapshot snapshot, HolderLookup.Provider lookup) {
        JsonObject maps = new JsonObject();
        for (var key : ForestryDataMaps.all()) maps.add(key.id().toString(), encodeMap(key, snapshot, lookup));
        return maps;
    }
    @SuppressWarnings("unchecked")
    private static <K, V> JsonObject encodeMap(DataMapKey<K, V> key, DataMapSnapshot snapshot, HolderLookup.Provider lookup) {
        JsonObject values = new JsonObject();
        snapshot.values().getOrDefault(key.id(), Map.of()).forEach((id, value) ->
            values.add(id.toString(), key.codec().encodeStart(lookup.createSerializationContext(JsonOps.INSTANCE), (V) value).getOrThrow()));
        JsonObject document = new JsonObject();
        document.addProperty("schema", 2);
        document.add("values", values);
        return document;
    }
    public static DataMapSnapshot decode(JsonObject maps, long revision, HolderLookup.Provider lookup) {
        var definitions = ForestryDataMaps.all();
        if (maps.size() != definitions.size()) throw new IllegalArgumentException("Incompatible Forestry DataMap set");
        var documents = new LinkedHashMap<DataMapKey<?, ?>, List<DataMapLoader.Document>>();
        for (var key : definitions) {
            var json = maps.getAsJsonObject(key.id().toString());
            if (json == null) throw new IllegalArgumentException("Missing Forestry DataMap: " + key.id());
            documents.put(key, List.of(new DataMapLoader.Document("server snapshot", json)));
        }
        return DataMapLoader.parse(documents, lookup, revision);
    }
}
