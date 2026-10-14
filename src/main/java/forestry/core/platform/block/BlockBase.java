package forestry.core.platform.block;

import net.neoforged.neoforge.transfer.transaction.Transaction;

import forestry.api.agriculture.HorizontalDirection;
import forestry.core.platform.tile.TileBase;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import javax.annotation.Nullable;

public class BlockBase<P extends Enum<P> & IBlockType> extends BlockForestry implements EntityBlock {
	/**
	 * use this instead of {@link net.minecraft.world.level.block.HorizontalDirectionalBlock#FACING} so the blocks rotate in a circle instead of NSWE order.
	 */
	public static final EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class, HorizontalDirection.VALUES);

	public final P blockType;

	private static Block.Properties createProperties(Block.Properties properties) {
		return properties.strength(2.0f);
	}

	public BlockBase(Block.Properties properties, P blockType) {
		super(createProperties(properties));

		if (getStateDefinition().any().hasProperty(FACING)) {
			registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
		}

		this.blockType = blockType;

		blockType.getMachineProperties().setBlock(this);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public float getShadeBrightness(BlockState p_220080_1_, BlockGetter p_220080_2_, BlockPos p_220080_3_) {
		return 0.2F;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return getDefinition().createTileEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actualType) {
		if (actualType == this.blockType.getMachineProperties().getTeType()) {
			//noinspection unchecked
			return (BlockEntityTicker<T>) (level.isClientSide() ? this.blockType.getMachineProperties().getClientTicker() : this.blockType.getMachineProperties().getServerTicker());
		} else {
			return null;
		}
	}

	private IMachineProperties<?> getDefinition() {
		return this.blockType.getMachineProperties();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
		IMachineProperties<?> definition = getDefinition();
		return definition.getShape(state, reader, pos, context);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player playerIn, BlockHitResult hit) {
		TileBase tile = TileUtil.getTile(worldIn, pos, TileBase.class);
		if (tile == null) {
			return InteractionResult.PASS;
		}
		if (TileUtil.isUsableByPlayer(playerIn, tile)) {
			if (!worldIn.isClientSide()) {
				ServerPlayer sPlayer = (ServerPlayer) playerIn;
				tile.openGui(sPlayer, InteractionHand.MAIN_HAND, pos);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level worldIn, BlockPos pos, Player playerIn, InteractionHand hand, BlockHitResult hit) {
		if (!playerIn.isShiftKeyDown()) {
			try (var tx = Transaction.openRoot()) {
				if (FluidUtil.interactWithFluidHandler(playerIn, hand, worldIn, pos, hit.getDirection(), tx)) {
					if (!worldIn.isClientSide()) tx.commit();
					return worldIn.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
				}
			}
		}
		return useWithoutItem(state, worldIn, pos, playerIn, hit).consumesAction()
			? (worldIn.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME)
			: InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		level.updateNeighbourForOutputSignal(pos, this);
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		Direction facing = state.getValue(FACING);
		return state.setValue(FACING, rot.rotate(facing));
	}
}
