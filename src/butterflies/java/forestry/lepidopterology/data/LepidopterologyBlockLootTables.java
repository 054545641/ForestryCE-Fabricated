package forestry.lepidopterology.data;

import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import forestry.lepidopterology.features.LepidopterologyBlocks;

/**
 * Generates the block drop loot tables for the butterflies jar. Both cocoons drop through their own
 * block logic rather than through a table, so both tables are empty.
 */
public class LepidopterologyBlockLootTables extends BlockLootSubProvider {
	protected LepidopterologyBlockLootTables(HolderLookup.Provider registries) {
		super(Set.of(), FeatureFlags.DEFAULT_FLAGS, registries);
	}

	@Override
	protected void generate() {
		for (Block block : getKnownBlocks()) {
			add(block, noDrop());
		}
	}

	@Override
	protected List<Block> getKnownBlocks() {
		return List.of(LepidopterologyBlocks.COCOON.block(), LepidopterologyBlocks.COCOON_SOLID.block());
	}
}
