package forestry.core.content.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A full block of plywood, which lays its grain along the axis it was placed on.
 * <p>
 * The five overrides below repeat {@link BlockBurnable} verbatim. A pillar block has to extend
 * RotatedPillarBlock for its axis, so it cannot extend BlockBurnable as well.
 */
public class BlockPlywoodBlock extends RotatedPillarBlock implements forestry.core.platform.block.ForestryCombustible {
	// the rate vanilla gives its planks
	private static final int FLAMMABILITY = 45;

	public BlockPlywoodBlock(Properties properties) {
		super(properties);
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return FLAMMABILITY;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return FLAMMABILITY;
	}

	@Override
	public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return true;
	}

	@Override
	public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
		return true;
	}

	@Override
	public boolean onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
		return level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
	}
}
