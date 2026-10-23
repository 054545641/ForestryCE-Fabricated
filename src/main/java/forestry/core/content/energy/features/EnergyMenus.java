package forestry.core.content.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.content.energy.menu.BiogasEngineMenu;
import forestry.core.content.energy.menu.CombustionEngineMenu;
import forestry.core.content.energy.menu.PeatEngineMenu;
import forestry.core.content.energy.menu.SolarEngineMenu;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class EnergyMenus {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureMenuType<BiogasEngineMenu> ENGINE_BIOGAS = REGISTRY.menuType(BiogasEngineMenu::fromNetwork, "biogas_engine");
	// Deviation from 1.20.1: the menu was named "engine_combustion" there, renamed to follow the block id.
	public static final FeatureMenuType<CombustionEngineMenu> ENGINE_COMBUSTION = REGISTRY.menuType(CombustionEngineMenu::fromNetwork, "combustion_engine");
	public static final FeatureMenuType<PeatEngineMenu> ENGINE_PEAT = REGISTRY.menuType(PeatEngineMenu::fromNetwork, "peat_engine");
	// Deviation from 1.20.1: the menu was named "engine_solar" there. 1.21.1's engine blocks use the
	// "<type>_engine" suffix naming, so the menu id follows the block id.
	public static final FeatureMenuType<SolarEngineMenu> ENGINE_SOLAR = REGISTRY.menuType(SolarEngineMenu::fromNetwork, "solar_engine");
}
