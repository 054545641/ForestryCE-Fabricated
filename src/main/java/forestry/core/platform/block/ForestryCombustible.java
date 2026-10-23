package forestry.core.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface ForestryCombustible extends DirectionalFireSpread {
    int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face);
    boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face);
    boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction face);
    boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter);
}
