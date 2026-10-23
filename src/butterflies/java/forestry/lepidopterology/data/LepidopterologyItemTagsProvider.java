package forestry.lepidopterology.data;

import net.minecraft.world.item.Item;

import thedarkcolour.modkit.data.MKTagsProvider;

import forestry.api.ForestryTags;
import forestry.lepidopterology.features.LepidopterologyItems;

/**
 * Generates the item tag entries the butterflies jar contributes. The game merges a tag across every
 * pack that names it, so core ships the rest of {@code genetic_samples} from its own file.
 */
public class LepidopterologyItemTagsProvider {
	public static void addTags(MKTagsProvider<Item> tags) {
		tags.tag(ForestryTags.Items.GENETIC_SAMPLES).add(LepidopterologyItems.CATERPILLAR_GE.item());
	}
}
