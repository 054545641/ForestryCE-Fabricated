package forestry.core.content.backpacks.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.content.backpacks.gui.ContainerBackpack;
import forestry.core.content.backpacks.gui.ContainerNaturalistBackpack;

@FeatureProvider
public class BackpackMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.STORAGE);

	public static final FeatureMenuType<ContainerBackpack> BACKPACK = REGISTRY.menuType(ContainerBackpack::fromNetwork, "backpack");
	public static final FeatureMenuType<ContainerNaturalistBackpack> NATURALIST_BACKPACK = REGISTRY.menuType(ContainerNaturalistBackpack::fromNetwork, "naturalist_backpack");
}
