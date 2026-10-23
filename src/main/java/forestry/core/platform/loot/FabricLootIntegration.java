package forestry.core.platform.loot;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public final class FabricLootIntegration {
    public static void initialize() {
        var rules = new TreeMap<Identifier, java.util.List<Identifier>>();
        try (var stream = FabricLootIntegration.class.getResourceAsStream("/forestry-loot-injections.json")) {
            if (stream == null) throw new IllegalStateException("Missing Forestry loot injection manifest");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            json.entrySet().forEach(entry -> {
                var tables = new ArrayList<Identifier>();
                entry.getValue().getAsJsonArray().forEach(table -> tables.add(Identifier.parse(table.getAsString())));
                rules.put(Identifier.parse(entry.getKey()), java.util.List.copyOf(tables));
            });
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot load Forestry loot injection manifest", error);
        }
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            for (var addition : rules.getOrDefault(key.identifier(), java.util.List.of())) {
                builder.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                    .add(NestedLootTable.lootTableReference(ResourceKey.create(Registries.LOOT_TABLE, addition))));
            }
        });
        forestry.arboriculture.loot.FabricGrafterLoot.initialize();
    }
}
