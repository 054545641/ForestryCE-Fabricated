package forestry.mail.gui;

import net.minecraft.client.renderer.RenderPipelines;
import forestry.api.ForestryConstants;
import forestry.core.platform.config.Constants;
import forestry.core.platform.config.ForestryConfig;
import forestry.mail.carriers.players.POBoxInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MailboxInfoToast implements Toast {
	private static final long DISPLAY_TIME = 5000L;
	private static final long ICON_TIME = 500L;
	private static final Identifier BACKGROUND_SPRITE = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/mail_toast.png");

	private final Component title = Component.translatable("for.gui.mail.toast.title");
	private POBoxInfo poBox;
	private Visibility visibility = Visibility.HIDE;

	public MailboxInfoToast(POBoxInfo info) {
		this.poBox = info;
	}

	@Override public Visibility getWantedVisibility() { return this.visibility; }

	@Override public void update(ToastManager manager, long fullyVisibleForMs) {
		this.visibility = ForestryConfig.CLIENT.mailAlertsEnabled.get() && this.poBox.hasMail()
			&& fullyVisibleForMs < DISPLAY_TIME * manager.getNotificationDisplayTimeMultiplier() ? Visibility.SHOW : Visibility.HIDE;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long timeSinceLastVisible) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, 0, 0, width(), height(), 256, 256);

		List<Icons> icons = new ArrayList<>(2);
		if (!this.poBox.hasMail()) {
			icons.add(Icons.PLAYER_LETTER);
		} else {
			if (this.poBox.playerLetters() > 0) {
				icons.add(Icons.PLAYER_LETTER);
			}
			if (this.poBox.tradeLetters() > 0) {
				icons.add(Icons.TRADE_LETTER);
			}
		}
		icons.get((int) (timeSinceLastVisible / ICON_TIME % (long) icons.size())).render(graphics, 6, 8);
		graphics.text(font, this.title, 36, 7, 0xFFFFFFFF, false);
		graphics.text(
			font,
			Component.translatable("for.gui.mail.toast.message", this.poBox.playerLetters() + this.poBox.tradeLetters()),
			36,
			18,
			0xFFFFFFFF,
			false
		);

	}

	public static void addOrUpdate(ToastManager toastGui, POBoxInfo poBox, boolean modifiedThroughPlayer) {
		MailboxInfoToast toast = toastGui.getToast(MailboxInfoToast.class, NO_TOKEN);
		if (toast != null) {
			toast.poBox = poBox;
		} else if (!modifiedThroughPlayer) {
			toastGui.addToast(new MailboxInfoToast(poBox));
		}

	}

	public enum Icons {
		PLAYER_LETTER(0, 0),
		TRADE_LETTER(0, 1);

		private final int x;
		private final int y;

		Icons(int pX, int pY) {
			this.x = pX;
			this.y = pY;
		}

		public void render(GuiGraphicsExtractor graphics, int pX, int pY) {
			// todo verify this sprite is correct
			graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, pX, pY, 176 + this.x * 26, this.y * 15, 26, 15, 256, 256);
		}
	}
}
