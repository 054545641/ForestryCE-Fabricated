package forestry.apiculture.alveary.multiblock;

import forestry.apiculture.alveary.AlvearyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AlvearyFanBlockEntity extends AlvearyClimatizerBlockEntity {
	public AlvearyFanBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.FAN, pos, state, (byte) -1);
	}
}
