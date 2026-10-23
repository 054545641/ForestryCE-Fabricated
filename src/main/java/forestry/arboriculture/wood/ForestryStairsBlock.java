package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ForestryStairsBlock extends StairBlock implements IWoodTyped {
	private final boolean fireproof;
	private final IWoodType woodType;

	public ForestryStairsBlock(Properties properties, ForestryPlanksBlock plank) {
		super(plank.defaultBlockState(), properties);
		this.fireproof = plank.isFireproof();
		this.woodType = plank.getWoodType();
	}

	@Override
	public boolean isFireproof() {
		return this.fireproof;
	}

	@Override
	public IWoodType getWoodType() {
		return this.woodType;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.STAIRS;
	}

	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		if (this.fireproof) {
			return 0;
		}
		return 20;
	}

	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		if (this.fireproof) {
			return 0;
		}
		return 5;
	}
}
