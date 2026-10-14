package forestry.mail.postoffice;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Rolls back newly allocated inboxes along with their inventory contents and subscriptions. */
public final class PostalRegistryJournal<K, V> extends SnapshotJournal<Map<K, V>> {
	private final Map<K, V> entries;
	private final Consumer<V> attach;
	private final Consumer<V> detach;
	private final Runnable changed;
	public PostalRegistryJournal(Map<K, V> entries, Consumer<V> attach, Consumer<V> detach, Runnable changed) {
		this.entries = entries;
		this.attach = attach;
		this.detach = detach;
		this.changed = changed;
	}
	@Override protected Map<K, V> createSnapshot() { return new HashMap<>(this.entries); }
	@Override protected void revertToSnapshot(Map<K, V> original) {
		this.entries.forEach((key, value) -> { if (original.get(key) != value) this.detach.accept(value); });
		original.forEach((key, value) -> { if (this.entries.get(key) != value) this.attach.accept(value); });
		this.entries.clear();
		this.entries.putAll(original);
	}
	@Override protected void onRootCommit(Map<K, V> original) {
		if (!original.equals(this.entries)) this.changed.run();
	}
}
