package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class ForestryPlanksBlock extends Block implements IWoodTyped {
	public static Properties createWoodProperties(IWoodType woodType, Properties properties) {
		return properties.strength(woodType.getHardness(), woodType.getHardness() * 1.5F).sound(SoundType.WOOD);
	}

	private final boolean fireproof;
	private final IWoodType woodType;

	public ForestryPlanksBlock(Properties properties, boolean fireproof, IWoodType woodType) {
		super(createWoodProperties(woodType, properties));
		this.fireproof = fireproof;
		this.woodType = woodType;
	}

	public IWoodType getWoodType() {
		return this.woodType;
	}

	@Override
	public boolean isFireproof() {
		return this.fireproof;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.PLANKS;
	}

	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		if (this.fireproof) {
			return 0;
		}
		return 20;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		if (this.fireproof) {
			return 0;
		}
		return 5;
	}

}
