package forestry.modules;

import forestry.Forestry;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.IForestryModule;
import forestry.api.modules.IModuleManager;
import forestry.core.platform.network.PacketRegistry;
import forestry.core.platform.util.ModUtil;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.objectweb.asm.Type;

import java.util.*;

public class ForestryModuleManager implements IModuleManager {
	private final LinkedHashMap<Identifier, IForestryModule> loadedModules = new LinkedHashMap<>();
	private final LinkedHashMap<String, List<IForestryModule>> loadedModulesByMod = new LinkedHashMap<>();

	@Override
	public Collection<IForestryModule> getLoadedModules() {
		return Collections.unmodifiableCollection(this.loadedModules.values());
	}

	@Override
	public boolean isModuleLoaded(Identifier id) {
		return this.loadedModules.containsKey(id);
	}

	@Override
	public List<IForestryModule> getModulesForMod(String modId) {
		return Collections.unmodifiableList(this.loadedModulesByMod.getOrDefault(modId, List.of()));
	}

	private void loadModules() {
		LinkedHashMap<Identifier, IForestryModule> discovered = new LinkedHashMap<>();
		List<ModuleLoadOrder.Entry<Identifier>> entries = new ArrayList<>();
		for (List<IForestryModule> modules : discoverModules().values()) {
			for (IForestryModule module : modules) {
				discovered.put(module.getId(), module);
				entries.add(new ModuleLoadOrder.Entry<>(module.getId(), module.getModuleDependencies(), module.getModDependencies()));
			}
		}
		List<Identifier> order = ModuleLoadOrder.resolve(entries, ModList.get()::isLoaded,
			(id, missing) -> Forestry.LOGGER.warn("Skipping module {} with missing dependencies: {}", id, missing));
		for (Identifier id : order) {
			IForestryModule module = discovered.get(id);
			this.loadedModules.put(id, module);
			this.loadedModulesByMod.computeIfAbsent(id.getNamespace(), key -> new ArrayList<>()).add(module);
		}
	}

	// Called during mod construction by Forestry
	public void init() {
		loadModules();

		for (Map.Entry<Identifier, IForestryModule> entry : this.loadedModules.entrySet()) {
			String modId = entry.getKey().getNamespace();
			IEventBus modBus = ModuleUtil.getModBus(modId);
			ModContainer mod = ModList.get().getModContainerById(modId).orElseThrow();
			IForestryModule module = entry.getValue();

			module.registerEvents(modBus);
			modBus.addListener((RegisterPayloadHandlersEvent event) -> module.registerPackets(new PacketRegistry(event.registrar(mod.getModInfo().getVersion().toString()))));

			if (FMLEnvironment.getDist() == Dist.CLIENT) {
				module.registerClientHandler(handler -> handler.registerEvents(modBus));
			}
		}

		this.loadedModulesByMod.forEach((modid, modules) -> {
			Forestry.LOGGER.debug("Handling Forestry module loading for mod '{}' with {} modules: {}", modid, modules.size(), Arrays.toString(modules.toArray()));
		});
	}

	// Returns a map of mod IDs to modules, with core modules first in each mod list
	private static LinkedHashMap<String, List<IForestryModule>> discoverModules() {
		LinkedHashMap<String, List<IForestryModule>> modules = new LinkedHashMap<>();

		ModuleUtil.forEachAnnotated(Type.getType(ForestryModule.class), klass -> {
			IForestryModule module;
			try {
				module = klass.asSubclass(IForestryModule.class).getConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Failed to instantiate module class " + klass.getName(), e);
			} catch (ClassCastException e) {
				throw new RuntimeException("Cannot load class" + klass.getName() + " as a @ForestryModule, it does not implement IForestryModule", e);
			}
			String modId = module.getId().getNamespace();
			// Namespace of the id must be a modid
			if (!ModUtil.isModLoaded(modId)) {
				throw new RuntimeException("Module " + module.getClass() + " returned '" + module.getId() + "' for its ID namespace, but no mod with ID '" + modId + "' is loaded");
			}
			List<IForestryModule> modModules = modules.computeIfAbsent(modId, k -> new ArrayList<>());
			// Core modules load first
			if (module.isCore()) {
				modModules.add(0, module);
			} else {
				modModules.add(module);
			}
		});

		// classpath order must not change module and reload ordering
		modules.values().forEach(modModules -> modModules.sort(
				Comparator.comparing((IForestryModule module) -> !module.isCore())
						.thenComparing(module -> module.getId().toString())));

		return modules;
	}

	private boolean apiSetUp = false;

	public void setupApi() {
		if (this.apiSetUp) {
			return;
		}
		for (IForestryModule module : getLoadedModules()) {
			try {
				module.setupApi();
			} catch (Throwable t) {
				// this exception normally gets swallowed, so log it and rethrow
				Forestry.LOGGER.fatal("Module {} threw an error in its IForestryModule.setupApi method", module.getId(), t);
				throw new RuntimeException(t);
			}
		}
		this.apiSetUp = true;
	}
}
