package forestry.agriculture.multifarm.items;

import forestry.core.platform.util.TranslationKeys;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.agriculture.multifarm.blocks.MultifarmBlock;
import net.minecraft.ChatFormatting;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class ItemBlockFarm extends ItemBlockForestry<MultifarmBlock> {
	public ItemBlockFarm(MultifarmBlock block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (forestry.core.platform.client.ClientsideCode.hasShiftDown()) {
			tooltip.add(Component.translatable("block.forestry.farm.tooltip").withStyle(ChatFormatting.GRAY));
		} else {
			tooltip.add(Component.translatable(TranslationKeys.HOLD_SHIFT_FOR_DETAILS).withStyle(ChatFormatting.GRAY));
		}
	}
}
