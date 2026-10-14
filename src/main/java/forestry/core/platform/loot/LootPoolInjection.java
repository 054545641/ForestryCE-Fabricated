package forestry.core.platform.loot;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/** Adds a weighted choice while retaining pool order, conditions, rolls and functions. */
public final class LootPoolInjection {
	private LootPoolInjection() {
	}

	public static void append(LootTableLoadEvent event, String poolName, LootPoolEntryContainer entry) {
		if (event.getTable().getPool(poolName) == null) {
			return;
		}
		var ops = RegistryOps.create(JsonOps.INSTANCE, event.getRegistries());
		var table = LootTable.DIRECT_CODEC.encodeStart(ops, event.getTable()).getOrThrow().getAsJsonObject();
		for (var element : table.getAsJsonArray("pools")) {
			var pool = element.getAsJsonObject();
			if (pool.has("name") && poolName.equals(pool.get("name").getAsString())) {
				pool.getAsJsonArray("entries").add(LootPoolEntries.CODEC.encodeStart(ops, entry).getOrThrow());
				event.setTable(LootTable.DIRECT_CODEC.parse(ops, table).getOrThrow());
				return;
			}
		}
	}
}
