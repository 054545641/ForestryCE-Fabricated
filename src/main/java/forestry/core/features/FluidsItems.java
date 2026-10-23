package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.FluidContainerType;
import forestry.core.platform.item.FluidContainerItem;
import forestry.core.platform.registration.FeatureItemGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class FluidsItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.FLUIDS);

	public static final FeatureItemGroup<FluidContainerItem, FluidContainerType> CONTAINERS = REGISTRY
		.itemGroup(FluidContainerItem::new, FluidContainerType.values())
		.create();
}
