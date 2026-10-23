package forestry.core.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.engine.circuits.ContainerSolderingIron;
import forestry.core.content.analyzer.PortableAnalyzerMenu;
import forestry.core.content.analyzer.ContainerAnalyzer;
import forestry.core.content.burnbarrel.ContainerBurnBarrel;
import forestry.core.content.escritoire.ContainerEscritoire;
import forestry.core.platform.gui.ContainerNaturalistInventory;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class CoreMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final FeatureMenuType<PortableAnalyzerMenu> ALYZER = REGISTRY.menuType(PortableAnalyzerMenu::fromNetwork, "alyzer");
	public static final FeatureMenuType<ContainerAnalyzer> ANALYZER = REGISTRY.menuType(ContainerAnalyzer::fromNetwork, "analyzer");
	public static final FeatureMenuType<ContainerEscritoire> ESCRITOIRE = REGISTRY.menuType(ContainerEscritoire::fromNetwork, "escritoire");
	public static final FeatureMenuType<ContainerBurnBarrel> BURN_BARREL = REGISTRY.menuType(ContainerBurnBarrel::fromNetwork, "burn_barrel");
	public static final FeatureMenuType<ContainerNaturalistInventory> NATURALIST_INVENTORY = REGISTRY.menuType(ContainerNaturalistInventory::fromNetwork, "naturalist_inventory");
	public static final FeatureMenuType<ContainerSolderingIron> SOLDERING_IRON = REGISTRY.menuType(ContainerSolderingIron::fromNetwork, "soldering_iron");
}
