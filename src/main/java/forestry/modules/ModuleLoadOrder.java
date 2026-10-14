package forestry.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

final class ModuleLoadOrder {
	record Entry<K>(K id, List<K> dependencies, List<String> requiredMods) {
	}

	static <K> List<K> resolve(Collection<Entry<K>> entries, Predicate<String> isModLoaded, BiConsumer<K, String> onSkipped) {
		Map<K, Entry<K>> pending = new LinkedHashMap<>();
		for (Entry<K> entry : entries) {
			if (pending.putIfAbsent(entry.id(), entry) != null) {
				throw new IllegalStateException("Duplicate Forestry module ID: " + entry.id());
			}
		}

		boolean changed;
		do {
			changed = false;
			var iterator = pending.values().iterator();
			while (iterator.hasNext()) {
				Entry<K> entry = iterator.next();
				List<K> missingModules = entry.dependencies().stream().filter(id -> !pending.containsKey(id)).toList();
				List<String> missingMods = entry.requiredMods().stream().filter(isModLoaded.negate()).toList();
				if (!missingModules.isEmpty() || !missingMods.isEmpty()) {
					iterator.remove();
					onSkipped.accept(entry.id(), "modules=" + missingModules + ", mods=" + missingMods);
					changed = true;
				}
			}
		} while (changed);

		List<K> loaded = new ArrayList<>();
		while (!pending.isEmpty()) {
			changed = false;
			for (Entry<K> entry : pending.values()) {
				if (loaded.containsAll(entry.dependencies())) {
					loaded.add(entry.id());
					pending.remove(entry.id());
					changed = true;
					break;
				}
			}
			if (!changed) {
				throw new IllegalStateException("Cyclic Forestry module dependencies: " + pending.values());
			}
		}
		return List.copyOf(loaded);
	}
}
