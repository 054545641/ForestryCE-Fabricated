package forestry.core.platform.block;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Block-specific text forwarded by ItemBlockForestry to the native item tooltip entry. */
public interface IBlockTooltip {
	void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag);
}
