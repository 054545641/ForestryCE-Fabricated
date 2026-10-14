package forestry.core.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public interface IColoredBlock {
	int colorMultiplier(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex);
}
