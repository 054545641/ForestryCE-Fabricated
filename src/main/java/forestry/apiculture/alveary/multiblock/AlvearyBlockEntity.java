package forestry.apiculture.alveary.multiblock;

import forestry.apiculture.alveary.AlvearyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AlvearyBlockEntity extends AbstractAlvearyBlockEntity {

	public AlvearyBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.PLAIN, pos, state);
	}

	@Override
	public boolean allowsAutomation() {
		return true;
	}

	@Override
	public String patternTypeId() {
		return AlvearyPattern.PLAIN;
	}

}
