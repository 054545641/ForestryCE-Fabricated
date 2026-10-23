package forestry.core.platform.registration;

import net.minecraft.resources.Identifier;
import java.util.*;

/** Module ownership is explicit and does not depend on an FML scan or registry namespace inference. */
public final class ModFeatureRegistry {
    private static final Map<Identifier, FeatureRegistry> MODULES = new LinkedHashMap<>();
    private ModFeatureRegistry() {}

    public static FeatureRegistry create(Identifier id, String ownerModId) {
        FeatureRegistry registry = new FeatureRegistry(id, ownerModId);
        if (MODULES.putIfAbsent(id, registry) != null) throw new IllegalStateException("Duplicate module feature registry: " + id);
        return registry;
    }

    public static FeatureRegistry get(Identifier id) {
        return Objects.requireNonNull(MODULES.get(id), () -> "Module has not entered registration: " + id);
    }

    public static Map<Identifier, FeatureRegistry> getRegistries() { return Collections.unmodifiableMap(MODULES); }
    public static long featureCount() { return MODULES.values().stream().mapToLong(registry -> registry.getFeatures().size()).sum(); }
}
