package forestry.core.platform.item;

import forestry.core.platform.util.ItemTooltipUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ItemForestry extends Item {
	public ItemForestry(Item.Properties properties) {
		super(properties);
	}

	/** List-based extension point for existing Forestry item subclasses. */
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
		ItemTooltipUtil.addInformation(stack, tooltip);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context,
		net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> output, TooltipFlag flag) {
		super.appendHoverText(stack, context, display, output, flag);
		List<Component> lines = new java.util.ArrayList<>();
		appendHoverText(stack, context, lines, flag);
		lines.forEach(output);
	}
}
