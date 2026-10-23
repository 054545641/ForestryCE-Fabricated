package forestry.core.platform.tile;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.core.features.CoreTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import forestry.core.platform.tile.TileNaturalistChest;

public class TileLepidopteristChest extends TileNaturalistChest {
	public TileLepidopteristChest(BlockPos pos, BlockState state) {
		super(CoreTiles.LEPIDOPTERIST_CHEST.tileType(), pos, state, ForestrySpeciesTypes.BUTTERFLY);
	}
}
