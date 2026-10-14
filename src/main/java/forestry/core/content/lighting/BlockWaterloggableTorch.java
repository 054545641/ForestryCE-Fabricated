package forestry.core.content.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class BlockWaterloggableTorch extends TorchBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	/**
	 * Deviation from 1.20.1: 1.21.1's TorchBlock narrowed its particle parameter to SimpleParticleType, so the
	 * phosphor dust can no longer be handed to super. Super gets a vanilla flame purely to satisfy the block codec,
	 * and animateTick below is overridden to spawn this instead, which keeps the original look.
	 */
	private static final DustParticleOptions PHOSPHOR_PARTICLE = new DustParticleOptions(0xADFFE2, 0.8f);

	public BlockWaterloggableTorch(BlockBehaviour.Properties properties) {
		// Deviation from 1.20.1: the properties now come from the feature registry rather than being built here.
		super(ParticleTypes.SOUL_FIRE_FLAME, properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	// Phosphor Torches don't 'burn' per se so don't emit smoke
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double d0 = (double) pos.getX() + 0.5D;
		double d1 = (double) pos.getY() + 0.65D;
		double d2 = (double) pos.getZ() + 0.5D;
		level.addParticle(PHOSPHOR_PARTICLE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}
}
