package forestry.mail.data;

import net.minecraft.world.item.Item;

import thedarkcolour.modkit.data.MKTagsProvider;

import forestry.api.ForestryTags;
import forestry.mail.features.MailItems;

/**
 * Generates the item tag entries the mail jar contributes. Every stamp comes from this jar, so core
 * ships no {@code stamps} file of its own.
 */
public class MailItemTagsProvider {
	public static void addTags(MKTagsProvider<Item> tags) {
		tags.tag(ForestryTags.Items.STAMPS).add(MailItems.STAMPS.itemArray());
	}
}
