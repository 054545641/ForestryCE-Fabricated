package forestry.agriculture.farmlogic;

import forestry.api.agriculture.ICrop;
import forestry.api.agriculture.IFarmHousing;
import forestry.api.agriculture.IFarmType;
import forestry.api.agriculture.IFarmable;
import forestry.core.platform.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Collection;

public class FarmLogicInfernal extends FarmLogicHomogeneous {
	public FarmLogicInfernal(IFarmType properties, boolean isManual) {
		super(properties, isManual);
	}

	@Override
	public Collection<ICrop> harvest(Level level, IFarmHousing housing, Direction direction, int extent, BlockPos pos) {
		ArrayDeque<ICrop> crops = new ArrayDeque<>();
		for (int i = 0; i < extent; i++) {
			BlockPos position = translateWithOffset(pos.above(), direction, i);
			if (!level.hasChunkAt(position)) {
				break;
			}
			if (level.isEmptyBlock(pos)) {
				continue;
			}
			BlockState blockState = level.getBlockState(position);
			for (IFarmable farmable : getFarmables()) {
				ICrop crop = farmable.getCropAt(level, position, blockState);
				if (crop != null) {
					crops.addFirst(crop);
					break;
				}
			}

		}
		return crops;

	}

	@Override
	protected boolean maintainSeedlings(Level world, IFarmHousing farmHousing, BlockPos pos, Direction direction, int extent) {
		for (int i = 0; i < extent; i++) {
			BlockPos position = translateWithOffset(pos, direction, i);
			if (!world.hasChunkAt(position)) {
				break;
			}

			BlockState blockState = world.getBlockState(position);
			if (!world.isEmptyBlock(position) && !BlockUtil.isReplaceableBlock(blockState, world, position)) {
				continue;
			}

			BlockPos soilPosition = position.below();
			BlockState soilState = world.getBlockState(soilPosition);
			if (isAcceptedSoil(soilState)) {
				return trySetCrop(world, farmHousing, position, direction);
			}
		}

		return false;
	}

}
