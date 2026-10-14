package forestry.core.platform.gui;

import forestry.core.platform.render.ColourProperties;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class TextLayoutHelper {
	private static final int LINE_HEIGHT = 12;

	private final GuiForestry<?> screen;
	private final int defaultFontColor;
	private final Font font;

	public int column0;
	public int column1;
	public int column2;
	public int line;

	public TextLayoutHelper(GuiForestry<?> gui, ColourProperties fontColour) {
		this.screen = gui;
		this.font = gui.font();
		this.defaultFontColor = fontColour.get("gui.screen");
	}

	public void startPage(GuiGraphicsExtractor matrices) {
        this.line = LINE_HEIGHT;
		matrices.pose().pushMatrix();
	}

	public void startPage(GuiGraphicsExtractor matrices, int column0, int column1, int column2) {
		this.column0 = column0;
		this.column1 = column1;
		this.column2 = column2;

		startPage(matrices);
	}

	public int getLineY() {
		return this.line;
	}

	public void newLine() {
        this.line += LINE_HEIGHT;
	}

	public void newLineCompressed() {
        this.line += LINE_HEIGHT - 2;
	}

	public void newLine(int lineHeight) {
        this.line += lineHeight;
	}

	public void endPage(GuiGraphicsExtractor graphics) {
		graphics.pose().popMatrix();
	}

	public void drawRow(GuiGraphicsExtractor graphics, Component text0, Component text1, Component text2, int colour0, int colour1, int colour2) {
		drawLine(graphics, text0, this.column0, colour0);
		drawLine(graphics, text1, this.column1, colour1);
		drawLine(graphics, text2, this.column2, colour2);
	}

	public void drawRow(GuiGraphicsExtractor graphics, Component text0, Component text1, int colour0, int colour1) {
		drawLine(graphics, text0, this.column0, colour0);
		drawLine(graphics, text1, this.column1, colour1);
	}

	public void drawLine(GuiGraphicsExtractor graphics, Component text, int x) {
		drawLine(graphics, text, x, this.defaultFontColor);
	}

	public void drawCenteredLine(GuiGraphicsExtractor graphics, Component text, int x, int color) {
		drawCenteredLine(graphics, text, x, this.screen.getSizeX(), 0xff000000 | color);
	}

	public void drawCenteredLine(GuiGraphicsExtractor graphics, Component text, int x, int width, int color) {
		drawCenteredLine(graphics, text, x, 0, width, 0xff000000 | color);
	}

	public void drawCenteredLine(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
		graphics.text(this.font, text, this.screen.getGuiLeft() + x + getCenteredOffset(text, width), this.screen.getGuiTop() + y + this.line, 0xff000000 | color, false);
	}

	public void drawLine(GuiGraphicsExtractor graphics, String text, int x, int color) {
		drawLine(graphics, text, x, 0, 0xff000000 | color);
	}

	public void drawLine(GuiGraphicsExtractor graphics, Component text, int x, int color) {
		drawLine(graphics, text, x, 0, 0xff000000 | color);
	}

	public void drawLine(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
		graphics.text(this.font, text, this.screen.getGuiLeft() + x, this.screen.getGuiTop() + y + this.line, 0xff000000 | color, false);
	}

	public void drawLine(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
		graphics.text(this.font, text, this.screen.getGuiLeft() + x, this.screen.getGuiTop() + y + this.line, 0xff000000 | color, false);
	}

	public void drawSplitLine(GuiGraphicsExtractor graphics, String text, int x, int maxWidth, int color) {
		drawSplitLine(graphics, Component.literal(text), x, maxWidth, 0xff000000 | color);
	}

	public void drawSplitLine(GuiGraphicsExtractor graphics, Component text, int x, int maxWidth, int color) {
		graphics.textWithWordWrap(this.font, text, this.screen.getGuiLeft() + x, this.screen.getGuiTop() + this.line, maxWidth, 0xff000000 | color);
	}

	public int getCenteredOffset(Component text) {
		return getCenteredOffset(text, this.screen.getSizeX());
	}

	public int getCenteredOffset(Component text, int xWidth) {
		return (xWidth - this.screen.font().width(text)) / 2;
	}
}
