package forestry.arboriculture.sapling;

import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.arboriculture.trees.TileTreeContainer;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.List;

public class SaplingBlock extends Block implements BonemealableBlock, EntityBlock {
	protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D);

	public SaplingBlock(Block.Properties properties) {
		super(properties.noCollision().strength(0.0F).sound(SoundType.GRASS).randomTicks());
	}

	@Override
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
		if (rand.nextFloat() > 0.1) {
			return;
		}

		TileTreeContainer tile = TileUtil.getTile(world, pos, TileTreeContainer.class);
		if (tile == null) {
			return;
		}

		tile.onBlockTick(world, pos, state, rand);
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockReader, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SaplingBlockEntity(pos, state);
	}

	/* PLANTING */
	public static boolean canBlockStay(BlockGetter level, BlockPos pos) {
		SaplingBlockEntity tile = TileUtil.getTile(level, pos, SaplingBlockEntity.class);
		if (tile == null) {
			return false;
		}

		ITree tree = tile.getTree();
		return tree != null && tree.canStay(level, pos);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, @javax.annotation.Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean isMoving) {
		if (!level.isClientSide() && !canBlockStay(level, pos)) {
			level.destroyBlock(pos, true);
		}
	}

	@Override
	public List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
		ItemStack drop = getDrop(builder.getParameter(LootContextParams.BLOCK_ENTITY));
		if (!drop.isEmpty()) {
			return Collections.singletonList(drop);
		}
		return Collections.emptyList();
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
		SaplingBlockEntity sapling = TileUtil.getTile(level, pos, SaplingBlockEntity.class);
		if (sapling == null || sapling.getTree() == null) {
			return ItemStack.EMPTY;
		}
		return SpeciesUtil.TREE_TYPE.get().createStack(sapling.getTree(), TreeLifeStage.SAPLING);
	}

	private static ItemStack getDrop(BlockEntity blockEntity) {
		if (blockEntity instanceof SaplingBlockEntity sapling) {
			ITree tree = sapling.getTree();
			if (tree != null) {
				return SpeciesUtil.TREE_TYPE.get().createStack(tree, TreeLifeStage.SAPLING);
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level world, RandomSource rand, BlockPos pos, BlockState state) {
		if (world.getRandom().nextFloat() >= 0.45F) {
			return false;
		}
		SaplingBlockEntity saplingTile = TileUtil.getTile(world, pos, SaplingBlockEntity.class);
		return saplingTile == null || saplingTile.canAcceptBoneMeal(rand);
	}

	@Override
	public void performBonemeal(ServerLevel world, RandomSource rand, BlockPos pos, BlockState blockState) {
		SaplingBlockEntity saplingTile = TileUtil.getTile(world, pos, SaplingBlockEntity.class);
		if (saplingTile != null) {
			saplingTile.tryGrow(rand, true);
		}
	}
}
