package forestry.agriculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.agriculture.minifarm.blocks.MinifarmBlock;
import forestry.agriculture.minifarm.blocks.MinifarmBlockType;
import forestry.agriculture.minifarm.items.MinifarmBlockItem;
import forestry.core.platform.registration.*;

@FeatureProvider
public class MinifarmBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CULTIVATION);

	public static final FeatureBlockGroup<MinifarmBlock, MinifarmBlockType> MANAGED_PLANTER = REGISTRY.blockGroup((properties, type) -> new MinifarmBlock(properties, type, false), MinifarmBlockType.VALUES).item(MinifarmBlockItem::new).identifier("managed", FeatureGroup.IdentifierType.SUFFIX).create();
	public static final FeatureBlockGroup<MinifarmBlock, MinifarmBlockType> MANUAL_PLANTER = REGISTRY.blockGroup((properties, type) -> new MinifarmBlock(properties, type, true), MinifarmBlockType.VALUES).item(MinifarmBlockItem::new).identifier("manual", FeatureGroup.IdentifierType.SUFFIX).create();
}
