package forestry.apiculture.apiary;

import forestry.core.platform.block.BlockBase;
import net.minecraft.world.level.block.SoundType;

public class ApicultureBlock extends BlockBase<ApicultureBlockType> {
	public ApicultureBlock(Properties properties, ApicultureBlockType type) {
		super(properties.sound(SoundType.WOOD), type);
	}
}
