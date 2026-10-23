package forestry.apiculture.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.apiculture.alveary.AlvearyMenu;
import forestry.apiculture.alveary.AlvearyHygroregulatorMenu;
import forestry.apiculture.alveary.AlvearySieveMenu;
import forestry.apiculture.alveary.AlvearySwarmerMenu;
import forestry.apiculture.bees.BeeHousingMenu;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class ApicultureMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	public static final FeatureMenuType<AlvearyMenu> ALVEARY = REGISTRY.menuType(AlvearyMenu::fromNetwork, "alveary");
	public static final FeatureMenuType<AlvearyHygroregulatorMenu> ALVEARY_HYGROREGULATOR = REGISTRY.menuType(AlvearyHygroregulatorMenu::fromNetwork, "alveary_hygroregulator");
	public static final FeatureMenuType<AlvearySieveMenu> ALVEARY_SIEVE = REGISTRY.menuType(AlvearySieveMenu::fromNetwork, "alveary_sieve");
	public static final FeatureMenuType<AlvearySwarmerMenu> ALVEARY_SWARMER = REGISTRY.menuType(AlvearySwarmerMenu::fromNetwork, "alveary_swarmer");
	public static final FeatureMenuType<BeeHousingMenu> BEE_HOUSING = REGISTRY.menuType(BeeHousingMenu::fromNetwork, "bee_housing");
}
