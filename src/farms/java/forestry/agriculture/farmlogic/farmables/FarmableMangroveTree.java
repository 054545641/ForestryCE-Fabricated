package forestry.agriculture.farmlogic.farmables;

import com.google.common.collect.ImmutableSet;
import forestry.api.agriculture.ICrop;
import forestry.agriculture.farmlogic.crops.CropDestroy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FarmableMangroveTree extends FarmableSapling {
	public FarmableMangroveTree(Item germling, ImmutableSet<Item> windfall) {
		super(germling, windfall);
	}

	@Override
	public ICrop getCropAt(Level level, BlockPos pos, BlockState state) {
		if (state.getBlock() == Blocks.MANGROVE_ROOTS || state.getBlock() == Blocks.MUDDY_MANGROVE_ROOTS) {
			return new CropDestroy(level, state, pos, null);
		}
		return super.getCropAt(level, pos, state);
	}
}
