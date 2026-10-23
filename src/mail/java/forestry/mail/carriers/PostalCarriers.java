package forestry.mail.carriers;

import forestry.api.ForestryRegistries;
import forestry.api.mail.IPostalCarrier;
import forestry.api.modules.ForestryModuleIds;
import forestry.mail.carriers.players.PlayerPostalCarrier;
import forestry.mail.carriers.trading.TradeStationCarrier;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.Holder;
import forestry.core.platform.registration.RegistrationBatch;

@FeatureProvider
public class PostalCarriers {
	private static final RegistrationBatch<IPostalCarrier> POSTAL_CARRIERS = ModFeatureRegistry.get(ForestryModuleIds.MAIL).getRegistry(ForestryRegistries.POSTAL_CARRIER.key());

	public static final forestry.core.platform.registration.RegistryEntry<IPostalCarrier, PlayerPostalCarrier> PLAYER = POSTAL_CARRIERS.register("player", PlayerPostalCarrier::new);
	public static final forestry.core.platform.registration.RegistryEntry<IPostalCarrier, TradeStationCarrier> TRADER = POSTAL_CARRIERS.register("trader", TradeStationCarrier::new);
}
