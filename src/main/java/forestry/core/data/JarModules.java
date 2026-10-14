package forestry.core.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import forestry.core.platform.registration.FeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

/**
 * Which registry ids a set of modules registered. Read out of the live registries, so it cannot drift
 * from what the code registers. Used to scope automatic name generation to one jar.
 */
public final class JarModules {
	private JarModules() {
	}

	/**
	 * @param moduleIds The modules a jar owns. Ex. {@code Set.of(ForestryModuleIds.MAIL)}
	 * @return Every id those modules registered, in any registry
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Set<Identifier> ownedIds(Set<Identifier> moduleIds) {
		Set<Identifier> ids = new HashSet<>();
		Set<Identifier> registered = new HashSet<>();
		for (ModFeatureRegistry modRegistry : ModFeatureRegistry.getRegistries().values()) {
			for (Map.Entry<Identifier, FeatureRegistry> module : modRegistry.getModules().entrySet()) {
				if (!moduleIds.contains(module.getKey())) {
					continue;
				}
				for (Map.Entry<ResourceKey, DeferredRegister> registry : module.getValue().getRegistries().entrySet()) {
					for (DeferredHolder<?, ?> holder : (Collection<DeferredHolder<?, ?>>) registry.getValue().getEntries()) {
						ids.add(holder.getId());
						registered.add(module.getKey());
					}
				}
			}
		}
		// A module named here that registered nothing is a typo or a rename, and it would silently scope
		// a jar's generation to less than it owns. Checked one module at a time, since the others in the
		// same set would cover for it
		if (!registered.containsAll(moduleIds)) {
			Set<Identifier> silent = new TreeSet<>(moduleIds);
			silent.removeAll(registered);
			throw new IllegalStateException("No registered ids for modules: " + silent);
		}
		return ids;
	}
}
