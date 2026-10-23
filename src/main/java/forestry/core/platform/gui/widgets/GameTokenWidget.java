package forestry.core.platform.gui.widgets;

import net.minecraft.client.renderer.RenderPipelines;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.gui.GuiUtil;
import forestry.core.platform.network.packets.PacketGuiSelectRequest;
import forestry.core.content.escritoire.EscritoireGame;
import forestry.core.content.escritoire.EscritoireGameToken;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.platform.util.SoundUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

public class GameTokenWidget extends Widget {
	private final ItemStack HIDDEN_TOKEN = new ItemStack(Items.BOOK);

	private final EscritoireGame game;
	private final int index;

	public GameTokenWidget(EscritoireGame game, WidgetManager manager, int xPos, int yPos, int index) {
		super(manager, xPos, yPos);
		this.game = game;
		this.index = index;
		this.width = 20;
		this.height = 20;
	}

	@Nullable
	private EscritoireGameToken getToken() {
		return this.game.getToken(this.index);
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		EscritoireGameToken token = getToken();
		if (token == null) {
			return;
		}

		int tokenColour = token.getTokenColour();
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.manager.gui.textureFile, startX + this.xPos, startY + this.yPos, 228, 0, 22, 22, 256, 256, 0xff000000 | tokenColour);

		ItemStack tokenStack = this.HIDDEN_TOKEN;
		if (token.isVisible()) {
			tokenStack = token.getTokenStack();
		}

		GuiUtil.drawItemStack(graphics, this.manager.gui, tokenStack, startX + this.xPos + 3, startY + this.yPos + 3);

		Identifier overlayToken = token.getOverlayToken();

		if (overlayToken != null) {
			TextureAtlasSprite icon = IForestryClientApi.INSTANCE.getTextureManager().getSprite(overlayToken);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, startX + this.xPos + 3, startY + this.yPos + 3, 16, 16);
		}
	}

	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		EscritoireGameToken token = getToken();
		if (token == null || !token.isVisible()) {
			return null;
		}

		ToolTip tooltip = new ToolTip();
		tooltip.add(token.getTooltip());
		return tooltip;
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
        this.game.choose(this.index);
		NetworkUtil.sendToServer(new PacketGuiSelectRequest(this.index, 0));
		SoundUtil.playButtonClick();
	}
}
