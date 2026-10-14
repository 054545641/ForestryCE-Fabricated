package forestry.core.platform.block;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class BlockTesr<P extends Enum<P> & IBlockType> extends BlockBase<P> {
	public BlockTesr(P blockType, Properties properties) {
		super(properties.noOcclusion(), blockType);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}
}
