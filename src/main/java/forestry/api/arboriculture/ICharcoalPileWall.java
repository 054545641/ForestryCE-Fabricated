package forestry.api.arboriculture;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

@Deprecated
public interface ICharcoalPileWall {

	int getCharcoalAmount();

	boolean matches(BlockState state);

	NonNullList<ItemStack> getDisplayItems();

}
