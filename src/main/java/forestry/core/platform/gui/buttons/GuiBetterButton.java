package forestry.core.platform.gui.buttons;

import net.minecraft.client.renderer.RenderPipelines;
import forestry.api.ForestryConstants;
import forestry.core.platform.config.Constants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GuiBetterButton extends Button {
	public static final Identifier TEXTURE = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/buttons.png");

	protected final IButtonTextureSet texture;

	public GuiBetterButton(int x, int y, IButtonTextureSet texture, OnPress handler) {
		super(x, y, texture.getWidth(), texture.getHeight(), Component.empty(), handler, DEFAULT_NARRATION);
		this.texture = texture;
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mX, int mY, float partialTick) {
		int xOffset = this.texture.getX();
		int yOffset = this.texture.getY();
		int h = this.height;
		int w = this.width;

		// VANILLA COPY EXCEPT FOR TEXTURE AND COORDINATES
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), xOffset, yOffset + (!this.active ? 0 : this.isHoveredOrFocused() ? 2 : 1) * h, w, h, 256, 256, net.minecraft.util.ARGB.white(this.alpha));
		extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}
}
