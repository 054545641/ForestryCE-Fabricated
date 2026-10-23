package forestry.core.platform.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class Drawable {
	/* Final Attributes */
	//Position on the Texture
	public final int u;
	public final int v;
	//Rectangle Size
	public final int uWidth;
	public final int vHeight;
	//Texture
	public final Identifier textureLocation;
	//Texture Size
	private final int textureWidth;
	private final int textureHeight;

	public Drawable(Identifier textureLocation, int u, int v, int uWidth, int vHeight) {
		this(textureLocation, u, v, uWidth, vHeight, 256, 256);
	}

	public Drawable(Identifier textureLocation, int u, int v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
		this.u = u;
		this.v = v;
		this.uWidth = uWidth;
		this.vHeight = vHeight;
		this.textureLocation = textureLocation;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	public void draw(GuiGraphicsExtractor graphics, int yOffset, int xOffset) {
		draw(graphics, yOffset, this.uWidth, this.vHeight, xOffset);
	}

	public void draw(GuiGraphicsExtractor graphics, int yOffset, int width, int height, int xOffset) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureLocation, xOffset, yOffset, this.u, this.v, width, height, this.uWidth, this.vHeight, this.textureWidth, this.textureHeight);
	}
}
