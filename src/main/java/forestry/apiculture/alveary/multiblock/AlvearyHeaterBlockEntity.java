package forestry.apiculture.alveary.multiblock;

import forestry.apiculture.alveary.AlvearyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AlvearyHeaterBlockEntity extends AlvearyClimatizerBlockEntity {
	public AlvearyHeaterBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.HEATER, pos, state, (byte) 1);
	}
}
