package forestry.lepidopterology.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.lepidopterology.cocoons.BlockCocoon;
import forestry.lepidopterology.cocoons.BlockSolidCocoon;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.item.BlockItem;

@FeatureProvider
public class LepidopterologyBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.LEPIDOPTEROLOGY);

	public static final FeatureBlock<BlockCocoon, BlockItem> COCOON = REGISTRY.block(BlockCocoon::new, "cocoon");
	// used only in world generation
	public static final FeatureBlock<BlockSolidCocoon, BlockItem> COCOON_SOLID = REGISTRY.block(BlockSolidCocoon::new, "cocoon_solid");
}
