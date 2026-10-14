package forestry.environment;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import forestry.core.platform.loot.ConditionLootModifier;
import forestry.core.platform.loot.LootPoolInjection;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.LootTableLoadEvent;

public final class N5LootProbe {
    public static void loot(GameTestHelper helper) {
        var level = helper.getLevel();
        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        var json = JsonParser.parseString("""
            {"type":"minecraft:empty","pools":[
              {"name":"before","rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:apple"}]},
              {"name":"main","rolls":2,"bonus_rolls":0,
               "conditions":[{"condition":"minecraft:random_chance","chance":1.0}],
               "functions":[{"function":"minecraft:set_count","count":3}],
               "entries":[{"type":"minecraft:item","name":"minecraft:bone","weight":5}]}]}
            """);
        var table = LootTable.DIRECT_CODEC.parse(ops, json).getOrThrow();
        var original = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject();
        var event = new LootTableLoadEvent(level.registryAccess(), Identifier.fromNamespaceAndPath("forestry_environment", "injection"), table);
        LootPoolInjection.append(event, "missing", LootItem.lootTableItem(Items.DIAMOND).build());
        helper.assertTrue(event.getTable() == table, "Missing target pool leaves table untouched");
        LootPoolInjection.append(event, "main", LootItem.lootTableItem(Items.STICK).setWeight(2).build());
        LootPoolInjection.append(event, "main", LootItem.lootTableItem(Items.COAL).build());
        var encoded = LootTable.DIRECT_CODEC.encodeStart(ops, event.getTable()).getOrThrow().getAsJsonObject();
        var pools = encoded.getAsJsonArray("pools");
        helper.assertTrue(pools.size() == 2 && pools.get(0).equals(original.getAsJsonArray("pools").get(0)), "Unrelated pool and ordering are preserved");
        var main = pools.get(1).getAsJsonObject();
        var entries = main.getAsJsonArray("entries");
        helper.assertTrue(entries.size() == 3 && entries.get(0).equals(original.getAsJsonArray("pools").get(1).getAsJsonObject().getAsJsonArray("entries").get(0)), "Both modules append to the same weighted pool");
        var expectedMain = original.getAsJsonArray("pools").get(1).getAsJsonObject().deepCopy();
        expectedMain.remove("entries");
        var actualMain = main.deepCopy();
        actualMain.remove("entries");
        helper.assertTrue(expectedMain.equals(actualMain), "Rolls, conditions and functions survive injection");
        var params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        var drops = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        event.getTable().getRandomItemsRaw(params, drops::add);
        helper.assertTrue(drops.size() == 3 && drops.getFirst().is(Items.APPLE)
            && drops.get(1).getCount() == 3 && drops.get(2).getCount() == 3,
            "Native loot evaluation retains two rolls and count function instead of adding guaranteed drops");
        var modifierJson = JsonParser.parseString("""
            {"conditions":[],"table":"minecraft:chests/simple_dungeon","extensions":[]}
            """).getAsJsonObject();
        var defaultModifier = ConditionLootModifier.CODEC.codec().parse(ops, modifierJson).getOrThrow();
        helper.assertTrue(defaultModifier.priority() == IGlobalLootModifier.DEFAULT_PRIORITY, "Legacy modifier JSON defaults priority");
        modifierJson.addProperty("priority", 37);
        var modifier = ConditionLootModifier.CODEC.codec().parse(ops, modifierJson).getOrThrow();
        var roundTrip = ConditionLootModifier.CODEC.codec().parse(ops,
            ConditionLootModifier.CODEC.codec().encodeStart(ops, modifier).getOrThrow()).getOrThrow();
        helper.assertTrue(roundTrip.priority() == 37, "Explicit modifier priority survives codec round trip");
        helper.succeed();
    }
}
