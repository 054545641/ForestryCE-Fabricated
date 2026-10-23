package forestry.core.content.worktable.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.worktable.screens.WorktableMenu;

@FeatureProvider
public class WorktableMenus {
	public static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.WORKTABLE);

	public static final FeatureMenuType<WorktableMenu> WORKTABLE = REGISTRY.menuType(WorktableMenu::fromNetwork, "worktable");
}
