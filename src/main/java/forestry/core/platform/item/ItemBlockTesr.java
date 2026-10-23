package forestry.core.platform.item;

import net.minecraft.world.level.block.Block;

/** Machine item whose rendering is selected by its native item definition. */
public class ItemBlockTesr<B extends Block> extends ItemBlockForestry<B> {
	public ItemBlockTesr(B block, Properties builder) { super(block, builder); }
}
