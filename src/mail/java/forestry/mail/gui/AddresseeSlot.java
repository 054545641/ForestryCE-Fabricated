package forestry.mail.gui;

import net.minecraft.client.renderer.RenderPipelines;
import forestry.api.client.ForestrySprites;
import forestry.api.core.tooltips.ToolTip;
import forestry.api.mail.IPostalCarrier;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.core.platform.util.SoundUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class AddresseeSlot extends Widget {
	private final LetterMenu menu;

	public AddresseeSlot(WidgetManager widgetManager, int xPos, int yPos, LetterMenu menu) {
		super(widgetManager, xPos, yPos);
		this.menu = menu;
		this.width = 26;
		this.height = 15;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		IPostalCarrier carrier = this.menu.getCarrier();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, carrier.getSprite(), startX + this.xPos, startY + this.yPos, 32, 32);
	}

	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		ToolTip tooltip = new ToolTip();
		tooltip.translated(this.menu.getCarrier().getTranslationKey());
		return tooltip;
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		if (!this.menu.getLetter().isProcessed()) {
            this.menu.advanceCarrierType();
			SoundUtil.playButtonClick();
		}
	}
}
