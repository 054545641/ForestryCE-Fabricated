package forestry.modules;

import forestry.Forestry;
import forestry.api.modules.IForestryModule;
import forestry.api.modules.IModuleManager;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.platform.registration.RegistrationBatch;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Explicit, deterministic module discovery; optional add-ons are never named from core code. */
public final class ForestryModuleManager implements IModuleManager {
    public static final ForestryModuleManager INSTANCE = new ForestryModuleManager();
    private final Map<Identifier, IForestryModule> loaded = new LinkedHashMap<>();
    private final Map<String, List<IForestryModule>> byOwner = new LinkedHashMap<>();
    private boolean initialized;

    public void init() {
        if (this.initialized) throw new IllegalStateException("Forestry initialized twice");
        Map<Identifier, IForestryModule> discovered = new TreeMap<>();
        FabricLoader loader = FabricLoader.getInstance();
        for (var entry : loader.getEntrypointContainers("forestry:module", IForestryModule.class)) {
            IForestryModule module = entry.getEntrypoint();
            String actualOwner = entry.getProvider().getMetadata().getId();
            if (!actualOwner.equals(module.getOwnerModId())) throw new IllegalStateException("Module owner mismatch: " + module.getId());
            if (discovered.putIfAbsent(module.getId(), module) != null) throw new IllegalStateException("Duplicate module: " + module.getId());
        }
        List<ModuleLoadOrder.Entry<Identifier>> entries = discovered.values().stream()
            .map(module -> new ModuleLoadOrder.Entry<>(module.getId(), module.getModuleDependencies(), module.getModDependencies())).toList();
        List<Identifier> order = ModuleLoadOrder.resolve(entries, loader::isModLoaded,
            (id, missing) -> { throw new IllegalStateException("Installed Forestry module has missing dependencies: " + id + " " + missing); });
        for (Identifier id : order) {
            IForestryModule module = discovered.get(id);
            this.loaded.put(id, module);
            this.byOwner.computeIfAbsent(module.getOwnerModId(), ignored -> new ArrayList<>()).add(module);
            ModFeatureRegistry.create(id, module.getOwnerModId());
            module.register();
        }
        RegistrationBatch.bindAll();
        this.initialized = true;
        Forestry.LOGGER.info("FORESTRY_F2_REGISTERED modules={} features={}", this.loaded.keySet(), ModFeatureRegistry.featureCount());
    }

    @Override public Collection<IForestryModule> getLoadedModules() { return Collections.unmodifiableCollection(this.loaded.values()); }
    @Override public boolean isModuleLoaded(Identifier id) { return this.loaded.containsKey(id) || ModFeatureRegistry.getRegistries().containsKey(id); }
    @Override public List<IForestryModule> getModulesForMod(String modId) { return List.copyOf(this.byOwner.getOrDefault(modId, List.of())); }
}
