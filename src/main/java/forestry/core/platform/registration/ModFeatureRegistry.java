package forestry.core.platform.registration;

import forestry.modules.ModuleUtil;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModFeatureRegistry {
	// Maps module id to feature (needed because of Binnie)
	private static final LinkedHashMap<String, ModFeatureRegistry> MOD_REGISTRY = new LinkedHashMap<>();

	private final HashMap<Identifier, FeatureRegistry> modules = new LinkedHashMap<>();
	private final IEventBus modBus;

	private ModFeatureRegistry(String modId) {
		this.modBus = ModuleUtil.getModBus(modId);
	}

	public void register(IModFeature feature) {
		getRegistry(feature.getModuleId()).register(feature);
	}

	public static FeatureRegistry get(Identifier moduleId) {
		return MOD_REGISTRY.computeIfAbsent(moduleId.getNamespace(), ModFeatureRegistry::new).getRegistry(moduleId);
	}

	public static Map<String, ModFeatureRegistry> getRegistries() {
		return MOD_REGISTRY;
	}

	public FeatureRegistry getRegistry(Identifier moduleId) {
		return this.modules.computeIfAbsent(moduleId, key -> new FeatureRegistry(key, this.modBus));
	}

	public Map<Identifier, FeatureRegistry> getModules() {
		return Collections.unmodifiableMap(this.modules);
	}
}
