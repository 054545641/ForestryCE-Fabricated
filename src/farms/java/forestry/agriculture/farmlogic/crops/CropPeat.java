package forestry.agriculture.farmlogic.crops;

import forestry.core.features.CoreTerrain;

import forestry.core.platform.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CropPeat extends Crop {
	public CropPeat(Level world, BlockPos position) {
		super(world, position);
	}

	@Override
	protected boolean isCrop(Level world, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return CoreTerrain.PEAT.blockEqual(blockState);
	}

	@Override
	protected List<ItemStack> harvestBlock(Level level, BlockPos pos) {
		NonNullList<ItemStack> drops = NonNullList.create();
		drops.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("peat"))));

		BlockUtil.sendDestroyEffects(level, pos, level.getBlockState(pos));

		level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
		return drops;
	}
}
