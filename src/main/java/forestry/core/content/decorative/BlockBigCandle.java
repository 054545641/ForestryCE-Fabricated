package forestry.core.content.decorative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A candle half a block wide and three quarters of a block tall. Big candles do not stack.
 */
public class BlockBigCandle extends Block implements SimpleWaterloggedBlock {
	// todo Wouldn't it be cool if you could stack big candles on top of each other?
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private static final VoxelShape SHAPE = Shapes.box(0.25, 0, 0.25, 0.75, 0.75, 0.75);

	private final SimpleParticleType flame;

	public BlockBigCandle(BlockBehaviour.Properties properties, BlockTypeBigCandle type) {
		// Deviation from 1.20.1: the properties and the light level were built in this constructor there.
		// The registry owns them here, as it does for every other block in this tree
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any()
			.setValue(LIT, false)
			.setValue(WATERLOGGED, false));
		this.flame = type == BlockTypeBigCandle.REFRACTORY ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
		builder.add(WATERLOGGED);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) return;
		BlockJumboCandle.addParticlesAndSound(level, net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.4375f, 0), random, this.flame);
	}

	// Item ignition and empty-hand extinguishing use separate native interaction entries.
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!state.getValue(LIT)
			&& !player.isShiftKeyDown()
			&& !state.getValue(WATERLOGGED)) {

			if (stack.is(Items.FLINT_AND_STEEL)) {
				if (!level.isClientSide()) {
					level.setBlock(pos, state.setValue(LIT, true), 3);
					stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				}
				level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
				return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
			} else if (stack.is(Items.FIRE_CHARGE)) {
				if (!level.isClientSide()) {
					level.setBlock(pos, state.setValue(LIT, true), 3);
					if (!player.isCreative()) {
						stack.shrink(1);
					}
				}
				level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
				return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
			}
		}

		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
		return this.defaultBlockState().setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(LIT) && player.getMainHandItem().isEmpty()) {
			if (!level.isClientSide()) {
				level.setBlock(pos, state.setValue(LIT, false), 3);
				level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		if (state.getValue(WATERLOGGED) || !fluid.is(Fluids.WATER)) return false;
		if (state.getValue(LIT)) {
			level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
		}
		level.setBlock(pos, state.setValue(WATERLOGGED, true).setValue(LIT, false), 3);
		level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return true;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, net.minecraft.util.RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			state = state.setValue(LIT, false);
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean canStickTo(BlockState state, BlockState other) {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}
}
