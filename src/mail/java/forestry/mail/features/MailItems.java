package forestry.mail.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.tradestation.CatalogItem;
import forestry.mail.letters.LetterItem;
import forestry.mail.letters.ItemStamp;
import forestry.core.platform.registration.*;

@FeatureProvider
public class MailItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.MAIL);

	public static final FeatureItemGroup<ItemStamp, EnumStampDefinition> STAMPS = REGISTRY.itemGroup(ItemStamp::new, "stamp", EnumStampDefinition.VALUES);
	public static final FeatureItemTable<LetterItem, LetterItem.Size, LetterItem.State> LETTERS = REGISTRY.itemTable(LetterItem::new, LetterItem.Size.values(), LetterItem.State.values(), "letter");
	public static final FeatureItem<CatalogItem> CATALOGUE = REGISTRY.item(CatalogItem::new, "catalog");
}
