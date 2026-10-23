package forestry.core.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface IColoredBlock {
	int colorMultiplier(BlockState state, @Nullable BlockGetter level, @Nullable BlockPos pos, int tintIndex);
}
