package forestry.mail.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestry;
import forestry.core.platform.render.ColourProperties;
import forestry.core.platform.util.NetworkUtil;
import forestry.mail.network.packets.PacketTraderAddressRequest;
import forestry.mail.tradestation.TradeStationBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

public class TradeStationNamingScreen extends GuiForestry<TradeStationNamingMenu> {
	private final TradeStationBlockEntity tile;
	private EditBox addressNameField;

	public TradeStationNamingScreen(TradeStationNamingMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/trade_station_naming.png", container, inv, title, 176, 90);
		this.tile = container.getTile();
	}

	@Override
	public void init() {
		super.init();

        this.addressNameField = new EditBox(this.font, this.leftPos + 44, this.topPos + 39, 90, 14, Component.translatable("for.gui.mail.nametrader"));
        this.addressNameField.setCanLoseFocus(true);
        this.addressNameField.setTextColor(-1);
        this.addressNameField.setTextColorUneditable(-1);
        this.addressNameField.setBordered(true);
        this.addressNameField.setMaxLength(12);
        this.addressNameField.setValue(this.menu.getAddress().getName());
		addWidget(this.addressNameField);
		setInitialFocus(this.addressNameField);
        this.addressNameField.setEditable(true);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (key == GLFW.GLFW_KEY_ENTER && this.addressNameField.isFocused()) {
			setAddress();
			return true;
		}

		return this.addressNameField.keyPressed(event)
			|| this.addressNameField.canConsumeInput()
			|| super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

        this.textLayout.startPage(graphics);
        this.textLayout.newLine();
        this.textLayout.drawCenteredLine(graphics, Component.translatable("for.gui.mail.nametrader"), 0, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.text")));
        this.textLayout.newLine(38);
        this.textLayout.drawCenteredLine(graphics, Component.translatable("for.gui.mail.nametrader.finish"), 0, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.text")));
        this.textLayout.endPage(graphics);
        this.addressNameField.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	private void setAddress() {
		String address = this.addressNameField.getValue();
		if (StringUtils.isNotBlank(address)) {
			PacketTraderAddressRequest packet = new PacketTraderAddressRequest(this.tile, address);
			NetworkUtil.sendToServer(packet);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
	}
}
