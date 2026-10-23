package forestry.mail.tab;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.tab.ForestryCreativeTabs;
import forestry.core.platform.registration.FeatureCreativeTab;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.item.CreativeModeTab;
import forestry.mail.blocks.MailBlockType;
import forestry.mail.features.MailBlocks;
import forestry.mail.features.MailItems;
import forestry.mail.letters.LetterItem;

/**
 * The mail creative tab. Ordering keys are built from tab ids rather than tab objects so
 * this module does not depend on the others' holder classes.
 */
@FeatureProvider
public class MailCreativeTab {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);

	public static final FeatureCreativeTab MAIL = REGISTRY.creativeTab("mail", tab -> {
		tab.icon(() -> MailBlocks.BASE.stack(MailBlockType.MAILBOX));
		tab.displayItems(MailCreativeTab::addMailItems);
		tab.withTabsBefore(ForestryCreativeTabs.tabKey("storage"));
	});

	static void addMailItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		MailBlocks.BASE.getItems().forEach(items::accept);
		items.accept(MailItems.CATALOGUE);
		MailItems.STAMPS.getItems().forEach(items::accept);
		items.accept(MailItems.LETTERS.get(LetterItem.Size.EMPTY, LetterItem.State.FRESH));
	}
}
