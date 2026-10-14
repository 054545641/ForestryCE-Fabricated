package forestry.mail.postoffice;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

/** Shared journal for collected postage, including withdrawals during a delivery. */
public final class PostageLedger extends SnapshotJournal<Object2IntOpenHashMap<Item>> {
	private final Object2IntOpenHashMap<Item> stamps;
	private final Runnable changed;

	public PostageLedger(Object2IntOpenHashMap<Item> stamps, Runnable changed) {
		this.stamps = stamps;
		this.changed = changed;
	}
	public CompoundTag writeCollected() {
		CompoundTag result = new CompoundTag();
		this.stamps.forEach((item, count) -> {
			if (count > 0) result.putInt(BuiltInRegistries.ITEM.getKey(item).toString(), count);
		});
		return result;
	}
	public void readCollected(CompoundTag tag, java.util.function.BiConsumer<String, Integer> unknownItem) {
		Object2IntOpenHashMap<Item> decoded = new Object2IntOpenHashMap<>();
		for (String key : tag.keySet()) {
			int count = tag.getInt(key).orElseThrow(() -> new IllegalArgumentException("Invalid stamp count for " + key));
			if (count <= 0) continue;
			Identifier id = Identifier.tryParse(key);
			Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
			if (item == Items.AIR) unknownItem.accept(key, count);
			else decoded.put(item, count);
		}
		this.stamps.clear();
		this.stamps.putAll(decoded);
	}
	@Override protected Object2IntOpenHashMap<Item> createSnapshot() { return new Object2IntOpenHashMap<>(this.stamps); }
	@Override protected void revertToSnapshot(Object2IntOpenHashMap<Item> snapshot) {
		this.stamps.clear();
		this.stamps.putAll(snapshot);
	}
	@Override protected void onRootCommit(Object2IntOpenHashMap<Item> original) {
		if (!original.equals(this.stamps)) this.changed.run();
	}
}
