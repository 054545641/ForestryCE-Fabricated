package forestry.agriculture.farmlogic.crops;

import forestry.core.platform.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


import java.util.ArrayList;
import java.util.List;

public class CropChorusFlower extends Crop {
	private static final BlockState BLOCK_STATE = Blocks.CHORUS_FLOWER.defaultBlockState();

	public CropChorusFlower(Level world, BlockPos position) {
		super(world, position);
	}

	@Override
	protected boolean isCrop(Level world, BlockPos pos) {
		return world.getBlockState(pos).getBlock() == Blocks.CHORUS_FLOWER;
	}

	@Override
	protected List<ItemStack> harvestBlock(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			net.minecraft.world.level.block.Block.popResource(serverLevel, pos, new ItemStack(Blocks.CHORUS_FLOWER));
		}

		BlockUtil.sendDestroyEffects(level, pos, BLOCK_STATE);
		level.removeBlock(pos, false);

		return List.of();
	}
}