package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.soil.*;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.registration.*;
import net.minecraft.world.item.BlockItem;

/** Original soil registrations used by the farm domain, owned by the core JAR. */
public final class CoreTerrain {
    private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
    public static final FeatureBlock<BlockHumus, ItemBlockForestry<BlockHumus>> HUMUS = REGISTRY.block(BlockHumus::new, ItemBlockForestry::new, "humus");
    public static final FeatureBlock<BlockBogEarth, ItemBlockForestry<BlockBogEarth>> BOG_EARTH = REGISTRY.block(BlockBogEarth::new, ItemBlockForestry::new, "bog_earth");
    public static final FeatureBlock<BlockPeat, BlockItem> PEAT = REGISTRY.block(BlockPeat::new, "peat");
}
