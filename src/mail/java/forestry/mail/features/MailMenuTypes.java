package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.mail.gui.*;
import forestry.core.platform.registration.FeatureMenuType;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class MailMenuTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);

	public static final FeatureMenuType<CatalogMenu> CATALOGUE = REGISTRY.menuType(CatalogMenu::fromNetwork, "catalog");
	public static final FeatureMenuType<LetterMenu> LETTER = forestry.mail.MailFabricModule.LETTER_MENU;
	public static final FeatureMenuType<MailboxMenu> MAILBOX = REGISTRY.menuType(MailboxMenu::fromNetwork, "mailbox");
	public static final FeatureMenuType<StampCollectorMenu> STAMP_COLLECTOR = REGISTRY.menuType(StampCollectorMenu::fromNetwork, "stamp_collector");
	public static final FeatureMenuType<TradeStationNamingMenu> TRADE_NAME = REGISTRY.menuType(TradeStationNamingMenu::fromNetwork, "trade_name");
	public static final FeatureMenuType<TradeStationMenu> TRADER = REGISTRY.menuType(TradeStationMenu::fromNetwork, "trader");
}
