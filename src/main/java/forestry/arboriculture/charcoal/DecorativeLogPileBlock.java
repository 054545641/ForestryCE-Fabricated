package forestry.arboriculture.charcoal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class DecorativeLogPileBlock extends RotatedPillarBlock {
	public DecorativeLogPileBlock(Block.Properties properties) {
		super(properties
			.sound(SoundType.WOOD)
			.strength(1.5f)
			.noOcclusion());
	}

}
