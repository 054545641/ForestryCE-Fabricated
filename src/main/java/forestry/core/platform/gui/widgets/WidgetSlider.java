package forestry.core.platform.gui.widgets;

import forestry.core.platform.gui.Drawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class WidgetSlider extends Widget {
	private final Drawable texture;

	private int xOffset;
	private int yOffset;

	public WidgetSlider(WidgetManager manager, int xPos, int yPos, Drawable texture) {
		super(manager, xPos, yPos);
		this.texture = texture;
		this.width = texture.uWidth;
		this.height = texture.vHeight;
	}

	public void setOffset(int xOffset, int yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	public int getXOffset() {
		return this.xOffset;
	}

	public int getYOffset() {
		return this.yOffset;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
        this.texture.draw(graphics, startY + this.yPos + this.yOffset, startX + this.xPos + this.xOffset);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		int xPos = this.xPos + this.xOffset;
		int yPos = this.yPos + this.yOffset;
		return mouseX >= xPos && mouseX <= xPos + this.width && mouseY >= yPos && mouseY <= yPos + this.height;
	}
}
