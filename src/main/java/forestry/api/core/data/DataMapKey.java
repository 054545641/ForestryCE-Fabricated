package forestry.api.core.data;

import com.mojang.serialization.Codec;
import forestry.api.ForestryDataMaps;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

/** Forestry-owned typed data map identity; no loader-owned registry attachments. */
public record DataMapKey<K, V>(Identifier id, ResourceKey<Registry<K>> registry, Codec<V> codec) {
    public DataMapKey { java.util.Objects.requireNonNull(id); java.util.Objects.requireNonNull(registry); java.util.Objects.requireNonNull(codec); }
    @Nullable public V get(Holder<K> entry) { return ForestryDataMaps.current().get(this, entry.unwrapKey().orElseThrow().identifier()); }
}
