package forestry.core.platform.gui.widgets;

import forestry.core.platform.gui.Drawable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

public class WidgetScrollBar extends Widget {
	@Nullable
	private final Drawable background;
	private final WidgetSlider slider;
	private boolean visible;

	private int minValue;
	private int maxValue;
	private int step;

	private int currentValue;
	@Nullable
	private IScrollable listener;

	private boolean isScrolling;
	private boolean wasClicked;

	private int initialMouseClickY;

	public WidgetScrollBar(WidgetManager manager, int xPos, int yPos, int width, int height, Drawable sliderTexture) {
		super(manager, xPos, yPos);

		this.background = null;
		this.width = width;
		this.height = height;

        this.isScrolling = false;
        this.wasClicked = false;
        this.visible = true;
        this.slider = new WidgetSlider(manager, xPos, yPos, sliderTexture);
	}

	public WidgetScrollBar(WidgetManager manager, int xPos, int yPos, Drawable backgroundTexture, boolean hasBorder, Drawable sliderTexture) {
		super(manager, xPos, yPos);

		int offset = hasBorder ? 1 : 0;

		this.background = backgroundTexture;
		this.width = backgroundTexture.uWidth;
		this.height = backgroundTexture.vHeight;

        this.isScrolling = false;
        this.wasClicked = false;
        this.visible = true;
        this.slider = new WidgetSlider(manager, xPos + offset, yPos + offset, sliderTexture);
	}

	public void setParameters(IScrollable listener, int minValue, int maxValue, int step) {
		this.listener = listener;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.step = step;

		setValue(this.currentValue);
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public int getValue() {
		return Mth.clamp(this.currentValue, this.minValue, this.maxValue);
	}

	public int setValue(int value) {
        this.currentValue = Mth.clamp(value, this.minValue, this.maxValue);
		if (this.listener != null) {
            this.listener.onScroll(this.currentValue);
		}
		int offset;
		if (value >= this.maxValue) {
			offset = this.height - this.slider.height;
		} else if (value <= this.minValue) {
			offset = 0;
		} else {
			offset = (int) (((float) (this.currentValue - this.minValue) / (this.maxValue - this.minValue)) * (float) (this.height - this.slider.height));
		}
        this.slider.setOffset(0, offset);
		return this.currentValue;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		if (!isVisible()) {
			return;
		}
		if (this.background != null) {
            this.background.draw(graphics, startY + this.yPos, startX + this.xPos);
		}
        this.slider.draw(graphics, startX, startY);
	}

	@Override
	public void update(int mouseX, int mouseY) {
		if (!isVisible()) {
			return;
		}
		boolean mouseDown = Minecraft.getInstance().mouseHandler.isLeftPressed();

		if (this.listener == null || this.listener.isFocused(mouseX, mouseY)) {
			//			int wheel = Mouse.getDWheel();    //TODO - dwheel. Maybe need to hook into forge events now?
			//			if (wheel > 0) {	//TODO I think this needs to be changed through the gui in mouseScrolled
			//				setValue(currentValue - step);
			//				return;
			//			} else if (wheel < 0) {
			//				setValue(currentValue + step);
			//				return;
			//			}
		}

		//the position of the mouse relative to the position of the widget
		int y = mouseY - this.yPos;

		if (!mouseDown && this.wasClicked) {
            this.wasClicked = false;
		}

		//not clicked and scrolling -> stop scrolling
		if (!mouseDown && this.isScrolling) {
			this.isScrolling = false;
		}

		//clicked on the slider and scrolling
		if (this.isScrolling) {
			float range = (float) (this.maxValue - this.minValue);
			float value = (float) (y - this.initialMouseClickY) / (float) (this.height - this.slider.height);
			value *= range;
			if (value < (float) this.step / 2f) {
				setValue(this.minValue);
			} else if (value > this.maxValue - ((float) this.step / 2f)) {
				setValue(this.maxValue);
			} else {
				setValue((int) (this.minValue + (float) this.step * Math.round(value)));
			}
		}
		//clicked on the slider
		else if (this.slider.isMouseOver(mouseX, mouseY)) {
			if (mouseDown) {
                this.isScrolling = true;
                this.initialMouseClickY = y - this.slider.getYOffset();
			}
		}
		//clicked on the bar but not on the slider
		else if (mouseDown && !this.wasClicked && isMouseOver(mouseX, mouseY)) {
			float range = (float) (this.maxValue - this.minValue);
			float value = (float) (y - this.slider.height / 2.0D) / (float) (this.height - this.slider.height);
			value *= range;
			if (value < (float) this.step / 2f) {
				setValue(this.minValue);
			} else if (value > this.maxValue - ((float) this.step / 2f)) {
				setValue(this.maxValue);
			} else {
				setValue((int) (this.minValue + (float) this.step * Math.round(value)));
			}
            this.wasClicked = true;
		}
	}
}
