package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class ForestryFenceGateBlock extends FenceGateBlock implements IWoodTyped {
	private final boolean fireproof;
	private final IWoodType woodType;

	public ForestryFenceGateBlock(Properties properties, boolean fireproof, IWoodType woodType) {
		super(new net.minecraft.world.level.block.state.properties.WoodType("forestry:" + woodType.getSerializedName() + "_gate", net.minecraft.world.level.block.state.properties.BlockSetType.OAK, SoundType.WOOD, SoundType.HANGING_SIGN, woodType.getFenceGateCloseSound(), woodType.getFenceGateOpenSound()), createBlockProperties(fireproof, woodType, properties));
		this.fireproof = fireproof;
		this.woodType = woodType;
	}

	private static Properties createBlockProperties(boolean fireproof, IWoodType woodType, Properties properties) {
		// todo add mapColor
		var props = properties
			.forceSolidOn()
			.strength(woodType.getHardness(), woodType.getHardness() * 1.5F)
			.instrument(NoteBlockInstrument.BASS)
			.sound(SoundType.WOOD);
		if (!fireproof) {
			props.ignitedByLava();
		}
		return props;
	}

	@Override
	public boolean isFireproof() {
		return this.fireproof;
	}

	@Override
	public IWoodType getWoodType() {
		return this.woodType;
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

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.FENCE_GATE;
	}
}
