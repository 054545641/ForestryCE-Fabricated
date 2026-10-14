package forestry.mail.gui;

import forestry.api.ForestryRegistries;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostalCarrier;
import forestry.core.platform.config.Constants;
import forestry.core.platform.config.SessionVars;
import forestry.core.platform.gui.GuiForestry;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.input.KeyEvent;
import forestry.core.platform.gui.widgets.ItemStackWidget;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.render.ColourProperties;
import forestry.core.platform.util.NetworkUtil;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.inventory.LetterInventory;
import forestry.mail.network.packets.PacketLetterInfoRequest;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Locale;

public class LetterScreen extends GuiForestry<LetterMenu> {
	private final LetterInventory itemInventory;
	private final boolean isProcessedLetter;
	private boolean checkedSessionVars;

	private EditBox address;
	private MultiLineEditBox text;

	private boolean addressFocus;
	private boolean textFocus;
	private String lastSyncedRecipient = "";
	private IPostalCarrier lastSyncedCarrier;
	private String lastSyncedText = "";

	private final ArrayList<Widget> tradeInfoWidgets;

	public LetterScreen(LetterMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/letter.png", container, inv, title, 194, 227);

		this.itemInventory = container.getItemInventory();

		this.isProcessedLetter = container.getLetter().isProcessed();
		this.widgetManager.add(new AddresseeSlot(this.widgetManager, 16, 12, container));
		this.tradeInfoWidgets = new ArrayList<>();
	}

	@Override
	public void init() {
		String previousAddress = this.address == null ? null : this.address.getValue();
		super.init();

        this.address = new EditBox(this.minecraft.font, this.leftPos + 46, this.topPos + 13, 93, 13, Component.translatable("for.gui.mail.to"));
        this.address.setEditable(!this.isProcessedLetter);
		IMailAddress recipient = this.menu.getRecipient();
		if (previousAddress != null) {
			this.address.setValue(previousAddress);
		} else if (recipient != null) {
			this.address.setValue(recipient.getName());
		}
        this.lastSyncedRecipient = this.address.getValue();
        this.lastSyncedCarrier = this.menu.getCarrier();
        this.address.setResponder(recipientName -> setRecipient(recipientName, this.menu.getCarrier()));

        this.text = MultiLineEditBox.builder().setX(this.leftPos + 17).setY(this.topPos + 31)
			.build(this.font, 122, 57, Component.translatable("for.gui.mail.letter"));
        this.text.setCharacterLimit(128);
        this.text.active = !this.isProcessedLetter;
		if (!this.menu.getText().isEmpty()) {
            this.text.setValue(this.menu.getText());
		}
        this.lastSyncedText = this.text.getValue();
        this.text.setValueListener(this::setText);

		addWidget(this.address);
		addWidget(this.text);
		updateTextVisibility();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.isProcessedLetter && this.address.isFocused()) {
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_ESCAPE) {
				setFocused(null);
			} else if (event.key() == GLFW.GLFW_KEY_TAB && this.minecraft.getConnection() != null) {
				String value = this.address.getValue().toLowerCase(Locale.ROOT);
				this.minecraft.getConnection().getOnlinePlayers().stream()
					.map(info -> info.getProfile().name())
					.filter(name -> name.toLowerCase(Locale.ROOT).contains(value))
					.findFirst().ifPresent(this.address::setValue);
			} else {
				this.address.keyPressed(event);
			}
			return true;
		}
		if (this.text.visible && this.text.isFocused()) {
			if (event.key() == GLFW.GLFW_KEY_ESCAPE || (event.key() == GLFW.GLFW_KEY_ENTER && !event.hasShiftDown())) {
				setFocused(null);
			} else {
				this.text.keyPressed(event);
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (!this.isProcessedLetter && !this.checkedSessionVars) {
            this.checkedSessionVars = true;
			setFromSessionVars();
			String recipient = this.address.getValue();
			IPostalCarrier carrier = this.menu.getCarrier();
			setRecipient(recipient, carrier);
		}

		// Check for focus changes
		if (this.addressFocus != this.address.isFocused()) {
			String recipient = this.address.getValue();
			IPostalCarrier carrier = this.menu.getCarrier();
			setRecipient(recipient, carrier);
		}
        this.addressFocus = this.address.isFocused();
		if (this.lastSyncedCarrier != this.menu.getCarrier()) {
			setRecipient(this.address.getValue(), this.menu.getCarrier());
		}
		if (this.textFocus != this.text.isFocused()) {
			setText();
		}
        this.textFocus = this.text.isFocused();

		updateTextVisibility();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		updateTextVisibility();
		updateTradeInfoWidgets();
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		if (this.isProcessedLetter) {
			graphics.text(this.font, this.address.getValue(), this.leftPos + 49, this.topPos + 16, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.lettertext")));
			graphics.textWithWordWrap(this.font, Component.literal(this.text.getValue()), this.leftPos + 20, this.topPos + 34, 119, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.lettertext")));
		} else {
            this.address.extractRenderState(graphics, mouseX, mouseY, partialTicks);
			if (this.menu.getCarrier().equals(PostalCarriers.TRADER.value())) {
				drawTradePreview(graphics, 18, 32);
			} else {
                this.text.extractRenderState(graphics, mouseX, mouseY, partialTicks);
			}
		}
	}

	private void updateTextVisibility() {
		this.address.visible = !this.isProcessedLetter;
		this.text.visible = !this.isProcessedLetter && !this.menu.getCarrier().equals(PostalCarriers.TRADER.value());
		if (!this.text.visible && this.text.isFocused()) {
			setFocused(null);
		}
	}

	private void drawTradePreview(GuiGraphicsExtractor graphics, int x, int y) {

		Component infoString = null;
		if (this.menu.getTradeInfo() == null) {
			infoString = Component.translatable("for.gui.mail.no.trader");
		} else if (this.menu.getTradeInfo().tradegood().isEmpty()) {
			infoString = Component.translatable("for.gui.mail.nothing.to.trade");
		} else if (!this.menu.getTradeInfo().state().isOk()) {
			infoString = this.menu.getTradeInfo().state().getDescription();
		}

		if (infoString != null) {
			graphics.textWithWordWrap(this.font, infoString, this.leftPos + x, this.topPos + y, 119, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.lettertext")));
			return;
		}

		graphics.text(this.font, Component.translatable("for.gui.mail.pleasesend"), this.leftPos + x, this.topPos + y, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.lettertext")));


		graphics.text(this.font, Component.translatable("for.gui.mail.foreveryattached"), this.leftPos + x, this.topPos + y + 28, (0xff000000 | ColourProperties.INSTANCE.get("gui.mail.lettertext")));

	}

	private void updateTradeInfoWidgets() {
		clearTradeInfoWidgets();
		var info = this.menu.getTradeInfo();
		if (this.isProcessedLetter || !this.menu.getCarrier().equals(PostalCarriers.TRADER.value())
			|| info == null || info.tradegood().isEmpty() || !info.state().isOk()) return;
		addTradeInfoWidget(new ItemStackWidget(this.widgetManager, 18, 42, info.tradegood()));
		for (int i = 0; i < info.required().size(); i++) {
			addTradeInfoWidget(new ItemStackWidget(this.widgetManager, 18 + i * 18, 70, info.required().get(i)));
		}
	}

	private void addTradeInfoWidget(Widget widget) {
        this.tradeInfoWidgets.add(widget);
        this.widgetManager.add(widget);
	}

	private void clearTradeInfoWidgets() {
		for (Widget widget : this.tradeInfoWidgets) {
            this.widgetManager.remove(widget);
		}
        this.tradeInfoWidgets.clear();
	}

	@Override
	public void onClose() {
		String recipientName = this.address.getValue();
		IPostalCarrier carrier = this.menu.getCarrier();
		setRecipient(recipientName, carrier);
		setText();
		super.onClose();
	}

	private void setFromSessionVars() {
		if (SessionVars.getStringVar("mail.letter.recipient") == null) {
			return;
		}

		String recipient = SessionVars.getStringVar("mail.letter.recipient");
		String typeName = SessionVars.getStringVar("mail.letter.carrier");
		Identifier carrierId = typeName == null ? null : Identifier.tryParse(typeName);
		IPostalCarrier carrier = carrierId == null ? null : ForestryRegistries.POSTAL_CARRIER.getValue(carrierId);

		if (StringUtils.isNotBlank(recipient) && carrier != null) {
            this.menu.setCarrier(carrier);
            this.address.setValue(recipient);
		}

		SessionVars.clearStringVar("mail.letter.recipient");
		SessionVars.clearStringVar("mail.letter.carrier");
	}

	private void setRecipient(String recipientName, IPostalCarrier carrier) {
		if (this.isProcessedLetter) {
			return;
		}
		if (recipientName.equals(this.lastSyncedRecipient) && carrier == this.lastSyncedCarrier) {
			return;
		}

        this.lastSyncedRecipient = recipientName;
        this.lastSyncedCarrier = carrier;
		PacketLetterInfoRequest packet = new PacketLetterInfoRequest(recipientName, carrier);
		NetworkUtil.sendToServer(packet);
	}

	private void setText() {
		setText(this.text.getValue());
	}

	private void setText(String text) {
		if (this.isProcessedLetter || this.lastSyncedText.equals(text)) {
			return;
		}

        this.lastSyncedText = text;
        this.menu.setText(text);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.itemInventory);
		addHintLedger("letter");
	}
}
