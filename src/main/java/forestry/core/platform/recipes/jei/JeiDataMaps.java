package forestry.core.platform.recipes.jei;

import com.mojang.datafixers.util.Pair;
import forestry.api.ForestryDataMaps;
import forestry.api.core.data.DataMapKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class JeiDataMaps {
    public static <V> List<Pair<ItemStack, V>> entries(DataMapKey<Item, V> type) {
        var snapshot = ForestryDataMaps.current();
        return sortedMapEntries(type).map(key -> Pair.of(new ItemStack(BuiltInRegistries.ITEM.getValue(key)), snapshot.get(type, key.identifier()))).toList();
    }

    public static <V> Stream<ResourceKey<Item>> sortedMapEntries(DataMapKey<Item, V> type) {
        return ForestryDataMaps.current().values().getOrDefault(type.id(), Map.of()).keySet().stream()
            .sorted().map(id -> ResourceKey.create(Registries.ITEM, id));
    }

    public static <V> List<ItemStack> stacks(DataMapKey<Item, V> type) {
        return sortedMapEntries(type).map(key -> new ItemStack(BuiltInRegistries.ITEM.getValue(key))).toList();
    }
}
