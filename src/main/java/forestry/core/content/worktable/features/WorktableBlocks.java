package forestry.core.content.worktable.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IBlockFeature;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.worktable.blocks.WorktableBlock;
import forestry.core.content.worktable.blocks.WorktableBlockType;

@FeatureProvider
public class WorktableBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.WORKTABLE);

	public static final IBlockFeature<WorktableBlock, ItemBlockForestry<?>> WORKTABLE = REGISTRY.block((properties) -> new WorktableBlock(properties, WorktableBlockType.WORKTABLE), ItemBlockForestry::new, "worktable");
}
