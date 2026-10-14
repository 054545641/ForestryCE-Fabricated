package forestry.mail.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestry;
import forestry.core.platform.render.ColourProperties;
import forestry.mail.tradestation.TradeStationBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TradeStationScreen extends GuiForestry<TradeStationMenu> {
	private final TradeStationBlockEntity tile;

	public TradeStationScreen(TradeStationMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/trade_station.png", container, inv, title, 226, 220);
		this.tile = container.getTile();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Component name = this.tile.getTitle();
		graphics.text(this.font, name, this.textLayout.getCenteredOffset(name), 6, (0xFF000000 | ColourProperties.INSTANCE.get("gui.mail.text")));

		Component receive = Component.translatable("for.gui.mail.receive");
		graphics.text(this.font, receive, this.textLayout.getCenteredOffset(receive, 70) + 51, 45, (0xFF000000 | ColourProperties.INSTANCE.get("gui.mail.text")));

		Component send = Component.translatable("for.gui.mail.send");
		graphics.text(this.font, send, this.textLayout.getCenteredOffset(send, 70) + 51, 99, (0xFF000000 | ColourProperties.INSTANCE.get("gui.mail.text")));

		super.extractLabels(graphics, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		graphics.text(this.font, this.menu.getAddress().getName(), this.leftPos + 19, this.topPos + 22, (0xFF000000 | ColourProperties.INSTANCE.get("gui.mail.text")));
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("trade.station");
		addOwnerLedger(this.tile);
	}
}
