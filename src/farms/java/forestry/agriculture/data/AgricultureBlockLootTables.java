package forestry.agriculture.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import forestry.agriculture.features.MinifarmBlocks;
import forestry.agriculture.features.MultifarmBlocks;

/**
 * Generates the block drop loot tables for the farms jar. Every farm block and every planter drops
 * itself.
 */
public class AgricultureBlockLootTables extends BlockLootSubProvider {
	protected AgricultureBlockLootTables(HolderLookup.Provider registries) {
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
		List<Block> blocks = new ArrayList<>(MultifarmBlocks.FARM.getBlocks());
		blocks.addAll(List.of(MinifarmBlocks.MANAGED_PLANTER.blockArray()));
		blocks.addAll(List.of(MinifarmBlocks.MANUAL_PLANTER.blockArray()));
		return blocks;
	}
}
