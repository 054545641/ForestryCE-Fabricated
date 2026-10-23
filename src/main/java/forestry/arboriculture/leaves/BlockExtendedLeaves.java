package forestry.arboriculture.leaves;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ParticleUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class BlockExtendedLeaves extends LeavesBlock {
	public static final MapCodec<BlockExtendedLeaves> CODEC = simpleCodec(BlockExtendedLeaves::new);
	public static final ThreadLocal<Boolean> SKIP_EXTENDED_CHECKS = ThreadLocal.withInitial(() -> false);
	private static final ThreadLocal<byte[]> SURROUNDINGS = ThreadLocal.withInitial(() -> new byte[9 * 9 * 9]);
	private static final byte SUPPORTED_LEAVES = 0;
	private static final byte NO_LEAVES = -1;
	private static final byte UNSUPPORTED_LEAVES = -2;

	// true when a leaves block with distance 6 or below is nearby (extends range to ~10)
	public static final BooleanProperty SUPPORTED = BooleanProperty.create("supported");

	public BlockExtendedLeaves(Properties properties) {
		super(0.01f, properties);

		registerDefaultState(defaultBlockState().setValue(SUPPORTED, false));
	}

	@Override
	public MapCodec<? extends BlockExtendedLeaves> codec() {
		return CODEC;
	}

	@Override
	protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
		ParticleUtils.spawnParticleBelow(level, pos, random,
			ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, level.getClientLeafTintColor(pos)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SUPPORTED);
	}

	@Override
	public boolean isRandomlyTicking(BlockState state) {
		return super.isRandomlyTicking(state) && !state.getValue(SUPPORTED);
	}

	@Override
	protected boolean decaying(BlockState state) {
		return super.decaying(state) && !state.getValue(SUPPORTED);
	}

	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos currentPos,
		Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {
		BlockState newState = super.updateShape(state, level, ticks, currentPos, facing, facingPos, facingState, random);

		boolean oldSupported = newState.getValue(SUPPORTED);
		boolean newSupported = hasSupport(newState, level, currentPos);

		if (oldSupported != newSupported) {
			newState = newState.setValue(SUPPORTED, newSupported);
			ticks.scheduleTick(currentPos, this, 1);
		}

		return newState;
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState newState = updateDistance(state, level, pos);

		boolean shouldBeSupported = hasSupport(newState, level, pos);
		newState = newState.setValue(SUPPORTED, shouldBeSupported);

		if (newState != state) {
			level.setBlock(pos, newState, 3);

			if (shouldBeSupported != state.getValue(SUPPORTED)) {
				for (Direction direction : Direction.values()) {
					BlockPos neighborPos = pos.relative(direction);
					BlockState neighborState = level.getBlockState(neighborPos);

					if (neighborState.getBlock() instanceof BlockExtendedLeaves) {
						level.scheduleTick(neighborPos, neighborState.getBlock(), 1);
					}
				}
			}
		}
	}

	/** Same distance rule as vanilla, exposed for Forestry's world generation. */
	public static BlockState updateDistance(BlockState state, LevelReader level, BlockPos pos) {
		int distance = DECAY_DISTANCE;
		for (Direction direction : Direction.values()) {
			distance = Math.min(distance, LeavesBlock.getOptionalDistanceAt(level.getBlockState(pos.relative(direction))).orElse(DECAY_DISTANCE) + 1);
			if (distance == 1) break;
		}
		return state.setValue(DISTANCE, distance);
	}

	private static boolean hasSupport(BlockState state, LevelReader level, BlockPos pos) {
		if (state.getValue(DISTANCE) <= 6) {
			return true;
		}

		return SKIP_EXTENDED_CHECKS.get() || hasExtendedSupport(level, pos);
	}

	private static boolean hasExtendedSupport(LevelReader level, BlockPos pos) {
		byte[] surroundings = BlockExtendedLeaves.SURROUNDINGS.get();

		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		// there's probably a faster way than just re-adding each time
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		for (int xOffset = -4; xOffset <= 4; xOffset++) {
			cursor.setX(x + xOffset);
			for (int yOffset = -4; yOffset <= 4; yOffset++) {
				cursor.setY(y + yOffset);
				for (int zOffset = -4; zOffset <= 4; zOffset++) {
					cursor.setZ(z + zOffset);

					BlockState state = level.getBlockState(cursor);
					int index = ((xOffset + 4) * 9 + yOffset + 4) * 9 + zOffset + 4;

					if (state.hasProperty(DISTANCE)) {
						if (state.getValue(DISTANCE) <= 6) {
							// source of support
							surroundings[index] = SUPPORTED_LEAVES;
						} else {
							// needs support (connector)
							surroundings[index] = UNSUPPORTED_LEAVES;
						}
					} else {
						// cannot support (barrier)
						surroundings[index] = NO_LEAVES;
					}
				}
			}
		}

		// four propagation steps
		for (byte sustainedValue = 1; sustainedValue <= 4; ++sustainedValue) {
			for (int xOffset = -4; xOffset <= 4; ++xOffset) {
				for (int yOffset = -4; yOffset <= 4; ++yOffset) {
					for (int zOffset = -4; zOffset <= 4; ++zOffset) {
						int index = ((xOffset + 4) * 9 + yOffset + 4) * 9 + zOffset + 4;

						if (surroundings[index] == sustainedValue - 1) {
							checkAndSetSupport(surroundings, xOffset - 1, yOffset, zOffset, sustainedValue);
							checkAndSetSupport(surroundings, xOffset + 1, yOffset, zOffset, sustainedValue);
							checkAndSetSupport(surroundings, xOffset, yOffset - 1, zOffset, sustainedValue);
							checkAndSetSupport(surroundings, xOffset, yOffset + 1, zOffset, sustainedValue);
							checkAndSetSupport(surroundings, xOffset, yOffset, zOffset - 1, sustainedValue);
							checkAndSetSupport(surroundings, xOffset, yOffset, zOffset + 1, sustainedValue);
						}
					}
				}
			}
		}

		return surroundings[364] >= 0;
	}

	private static void checkAndSetSupport(byte[] array, int x, int y, int z, byte supportValue) {
		if (x < -4 || x > 4 || y < -4 || y > 4 || z < -4 || z > 4) {
			return;
		}

		int arrayIndex = ((x + 4) * 9 + y + 4) * 9 + z + 4;

		if (array[arrayIndex] == UNSUPPORTED_LEAVES) {
			array[arrayIndex] = supportValue;
		}
	}
}
