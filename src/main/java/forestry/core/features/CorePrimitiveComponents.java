package forestry.core.features;

import com.mojang.serialization.Codec;
import forestry.core.content.backpacks.BackpackMode;
import forestry.core.platform.registration.FeatureRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import java.util.List;

/** Existing primitive component IDs/codecs; genome and fluid schemas move with their F2 value types. */
public final class CorePrimitiveComponents {
    private CorePrimitiveComponents() {}
    public static void register(FeatureRegistry registry) {
        var components = registry.getRegistry(Registries.DATA_COMPONENT_TYPE);
        for (String id : List.of("analyzed", "bee_pristine")) {
            components.register(id, () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
        }
        for (String id : List.of("health", "max_health", "bee_generation", "alyzer_charges", "item_inventory_uid")) {
            components.register(id, () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
        }
        components.register("backpack_mode", BackpackMode::componentType);
    }
}
