package forestry.core.content.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Used for simple, burnable blocks. One of these catches fire on every face, spreads at the vanilla plank
 * rate, keeps a fire above it alight and turns straight into fire rather than charring away first.
 * <p>
 * Deviation from 1.20.1: that tree carried three constructors, two of which took a flammability and a
 * spread speed. Nothing called them, so the one rate below stands in for both fields.
 */
public abstract class BlockBurnable extends Block {
	// the rate vanilla gives its planks
	private static final int FLAMMABILITY = 45;

	public BlockBurnable(Properties properties) {
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
