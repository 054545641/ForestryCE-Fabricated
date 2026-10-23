package forestry.mail.data;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import forestry.mail.features.MailBlocks;

/**
 * Generates the block drop loot tables for the mail jar. All three machines drop themselves.
 */
public class MailBlockLootTables extends BlockLootSubProvider {
	protected MailBlockLootTables(HolderLookup.Provider registries) {
		super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
	}

	@Override
	protected void generate() {
		for (Block block : getKnownBlocks()) {
			dropSelf(block);
		}
	}

	@Override
	protected List<Block> getKnownBlocks() {
		return List.of(MailBlocks.BASE.blockArray());
	}
}
