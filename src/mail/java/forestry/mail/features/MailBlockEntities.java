package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.mail.blocks.MailBlockType;
import forestry.mail.postoffice.MailboxBlockEntity;
import forestry.mail.postoffice.StampCollectorBlockEntity;
import forestry.mail.tradestation.TradeStationBlockEntity;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureTileType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

@FeatureProvider
public class MailBlockEntities {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);

	public static final FeatureTileType<MailboxBlockEntity> MAILBOX = REGISTRY.tile(MailboxBlockEntity::new, "mailbox", () -> MailBlocks.BASE.get(MailBlockType.MAILBOX).collect());
	public static final FeatureTileType<StampCollectorBlockEntity> STAMP_COLLECTOR = REGISTRY.tile(StampCollectorBlockEntity::new, "stamp_collector", () -> MailBlocks.BASE.get(MailBlockType.STAMP_COLLETOR).collect());
	public static final FeatureTileType<TradeStationBlockEntity> TRADER = REGISTRY.tile(TradeStationBlockEntity::new, "trader", () -> MailBlocks.BASE.get(MailBlockType.TRADE_STATION).collect());
}
