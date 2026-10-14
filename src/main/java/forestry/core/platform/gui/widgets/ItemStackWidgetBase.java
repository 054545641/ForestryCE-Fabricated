package forestry.core.platform.gui.widgets;

import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.gui.GuiUtil;
import forestry.core.platform.util.ItemTooltipUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class ItemStackWidgetBase extends Widget {
	public ItemStackWidgetBase(WidgetManager widgetManager, int xPos, int yPos) {
		super(widgetManager, xPos, yPos);
	}

	protected abstract ItemStack getItemStack();

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		ItemStack itemStack = getItemStack();
		if (!itemStack.isEmpty()) {
			GuiUtil.drawItemStack(graphics, this.manager.gui, itemStack, this.xPos + startX, this.yPos + startY);
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		ItemStack itemStack = getItemStack();
		ToolTip tip = new ToolTip();
		if (!itemStack.isEmpty()) {
			tip.addAll(ItemTooltipUtil.getInformation(itemStack));
		}
		return tip;
	}
}
