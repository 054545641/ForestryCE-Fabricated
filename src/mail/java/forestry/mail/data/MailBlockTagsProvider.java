package forestry.mail.data;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import thedarkcolour.modkit.data.MKTagsProvider;

import forestry.mail.features.MailBlocks;

/**
 * Generates the block tag entries the mail jar contributes. The game merges a tag across every pack
 * that names it, so core ships the rest of {@code mineable/pickaxe} from its own file.
 */
public class MailBlockTagsProvider {
	public static void addTags(MKTagsProvider<Block> tags) {
		for (Block block : MailBlocks.BASE.blockArray()) {
			tags.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
		}
	}
}
