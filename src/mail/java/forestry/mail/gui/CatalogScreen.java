package forestry.mail.gui;

import forestry.api.mail.ITradeStationInfo;
import forestry.api.ForestryRegistries;
import forestry.core.platform.config.SessionVars;
import forestry.core.platform.gui.GuiForestry;
import forestry.core.platform.gui.widgets.ItemStackWidget;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.network.packets.PacketGuiSelectRequest;
import forestry.core.platform.render.ColourProperties;
import forestry.core.platform.util.NetworkUtil;
import forestry.mail.carriers.PostalCarriers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CatalogScreen extends GuiForestry<CatalogMenu> {
	private static final String BOLD_UNDERLINE = ChatFormatting.BOLD.toString() + ChatFormatting.UNDERLINE;

	@Nullable
	private Button buttonFilter;
	@Nullable
	private Button buttonUse;

	private final List<ItemStackWidget> tradeInfoWidgets = new ArrayList<>();

	public CatalogScreen(CatalogMenu container, Inventory inv, Component title) {
		super(Identifier.parse("textures/gui/book.png"), container, inv, title, 192, 192);

	}

	@Override
	public void init() {
		super.init();

		this.renderables.clear();

		addRenderableWidget(new Button.Builder(Component.literal(">"), b -> actionPerformed(2)).pos(this.width / 2 + 44, this.topPos + 150).size(12, 20).build());
		addRenderableWidget(new Button.Builder(Component.literal("<"), b -> actionPerformed(3)).pos(this.width / 2 - 58, this.topPos + 150).size(12, 20).build());

		this.buttonFilter = new Button.Builder(Component.translatable("for.gui.mail.filter.all"), b -> actionPerformed(4)).pos(this.width / 2 - 44, this.topPos + 150).size(42, 20).build();
		addRenderableWidget(this.buttonFilter);

		this.buttonUse = new Button.Builder(Component.translatable("for.gui.mail.address.copy"), b -> actionPerformed(5)).pos(this.width / 2, this.topPos + 150).size(42, 20).build();
		addRenderableWidget(this.buttonUse);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		updateTradeInfoWidgets();
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		graphics.text(this.font, String.format("%s / %s", this.menu.getPageNumber(), this.menu.getPageCount()), this.leftPos + this.imageWidth - 72, this.topPos + 12, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")), false);


		ITradeStationInfo tradeInfo = this.menu.getTradeInfo();

		if (this.buttonUse != null) {
			if (tradeInfo != null) {
				drawTradePreview(graphics, tradeInfo, this.leftPos + 38, this.topPos + 30);
				this.buttonUse.visible = tradeInfo.state().isOk();
			} else {
				drawNoTrade(graphics, this.leftPos + 38, this.topPos + 30);
				this.buttonUse.visible = false;
			}
		}

		if (this.buttonFilter != null) {
			this.buttonFilter.setMessage(Component.translatable("for.gui.mail.filter." + this.menu.getFilterIdent()));
		}
	}

	private void drawNoTrade(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.textWithWordWrap(this.font, Component.translatable("for.gui.mail.notrades"), x, y + 18, 119, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")));
	}

	private void drawTradePreview(GuiGraphicsExtractor graphics, ITradeStationInfo tradeInfo, int x, int y) {
		Font font = this.font;
		graphics.text(font, BOLD_UNDERLINE + tradeInfo.address().getName(), x, y, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")), false);

		graphics.text(font, Component.translatable("for.gui.mail.willtrade", tradeInfo.owner().name()), x, y + 18, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")), false);


		graphics.text(font, Component.translatable("for.gui.mail.tradefor"), x, y + 46, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")), false);


		if (tradeInfo.state().isOk()) {
			graphics.textWithWordWrap(font, tradeInfo.state().getDescription().copy().withStyle(ChatFormatting.DARK_GREEN), x, y + 82, 119, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")));
		} else {
			graphics.textWithWordWrap(font, tradeInfo.state().getDescription().copy().withStyle(ChatFormatting.DARK_RED), x, y + 82, 119, (0xff000000 | ColourProperties.INSTANCE.get("gui.book")));
		}
	}

	private void updateTradeInfoWidgets() {
		clearTradeInfoWidgets();
		ITradeStationInfo info = this.menu.getTradeInfo();
		if (info == null) return;
		addTradeInfoWidget(new ItemStackWidget(this.widgetManager, 38, 58, info.tradegood()));
		for (int i = 0; i < info.required().size(); i++) {
			addTradeInfoWidget(new ItemStackWidget(this.widgetManager, 38 + i * 18, 86, info.required().get(i)));
		}
	}

	private void addTradeInfoWidget(ItemStackWidget widget) {
        this.tradeInfoWidgets.add(widget);
        this.widgetManager.add(widget);
	}

	private void clearTradeInfoWidgets() {
		for (Widget widget : this.tradeInfoWidgets) {
            this.widgetManager.remove(widget);
		}
        this.tradeInfoWidgets.clear();
	}

	protected void actionPerformed(int id) {
		switch (id) {
			case 0 -> onClose();
			case 2 -> // next page
				NetworkUtil.sendToServer(new PacketGuiSelectRequest(0, 0));
			case 3 -> // previous page
				NetworkUtil.sendToServer(new PacketGuiSelectRequest(1, 0));
			case 4 -> // cycle filter
				NetworkUtil.sendToServer(new PacketGuiSelectRequest(2, 0));
			case 5 -> {
				ITradeStationInfo info = this.menu.getTradeInfo();
				if (info != null) {
					SessionVars.setStringVar("mail.letter.recipient", info.address().getName());
					SessionVars.setStringVar("mail.letter.carrier", ForestryRegistries.POSTAL_CARRIER.getKey(PostalCarriers.TRADER.value()).toString());
				}
				this.minecraft.gui.hud.getChat().addClientSystemMessage(Component.translatable("for.gui.mail.catalogue.save_info"));
				onClose();
			}
		}
	}

	@Override
	protected void addLedgers() {
	}
}
