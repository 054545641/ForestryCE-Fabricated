package forestry.core.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** The direction-sensitive spreading rule predates Fabric and is not expressible by a constant fuel entry. */
public interface DirectionalFireSpread {
    int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face);
}
