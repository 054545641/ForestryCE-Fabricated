package forestry.core.platform.gui;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import forestry.Forestry;
import forestry.api.ForestryConstants;
import forestry.api.core.climate.IClimateProvider;
import forestry.api.core.IErrorLogicSource;
import forestry.api.core.IErrorSource;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.gui.ledgers.*;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.core.platform.owner.IOwnedTile;
import forestry.core.platform.render.ColourProperties;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.joml.Matrix3x2fStack;

import javax.annotation.Nullable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public abstract class GuiForestry<C extends AbstractContainerMenu> extends AbstractContainerScreen<C> implements IGuiSizable {
	// Used to display "Did you know?" ledgers in GUI
	public static final LinkedListMultimap<String, String> HINTS = readDefaultHints();

	public final Identifier textureFile;
	protected final WidgetManager widgetManager;
	protected final LedgerManager ledgerManager;
	protected TextLayoutHelper textLayout;

	protected GuiForestry(String texture, C menu, Inventory inv, Component title) {
		this(ForestryConstants.forestry(texture), menu, inv, title);
	}

	protected GuiForestry(Identifier texture, C menu, Inventory inv, Component title) {
		this(texture, menu, inv, title, 176, 166);
	}

	protected GuiForestry(String texture, C menu, Inventory inv, Component title, int width, int height) {
		this(ForestryConstants.forestry(texture), menu, inv, title, width, height);
	}

	protected GuiForestry(Identifier texture, C menu, Inventory inv, Component title, int width, int height) {
		super(menu, inv, title, width, height);

		this.widgetManager = new WidgetManager(this);
		this.ledgerManager = new LedgerManager(this);

		this.textureFile = texture;
	}

	/* LEDGERS */
	@Override
	public void init() {
		super.init();

		int maxLedgerWidth = (this.width - this.imageWidth) / 2;

		this.ledgerManager.setMaxWidth(maxLedgerWidth);
		this.ledgerManager.clear();

		this.textLayout = new TextLayoutHelper(this, ColourProperties.INSTANCE);

		addLedgers();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
	}

	@Override
	public void containerTick() {
		super.containerTick();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		extractTooltip(graphics, mouseX, mouseY);
	}

	protected abstract void addLedgers();

	protected final void addErrorLedger(IErrorSource errorSource) {
        this.ledgerManager.add(errorSource);
	}

	protected final void addErrorLedger(IErrorLogicSource errorSource) {
        this.ledgerManager.add(errorSource.getErrorLogic());
	}

	protected final void addClimateLedger(IClimateProvider climatised) {
        this.ledgerManager.add(new ClimateLedger(this.ledgerManager, climatised));
	}

	protected final void addPowerLedger(ForestryEnergyStorage energyStorage) {
        this.ledgerManager.add(new PowerLedger(this.ledgerManager, energyStorage));
	}

	protected final void addHintLedger(String hintsKey) {
		if (ForestryConfig.CLIENT.enableHints.get()) {
			List<String> hints = HINTS.get(hintsKey);
			addHintLedger(hints);
		}
	}

	protected final void addHintLedger(List<String> hints) {
		if (ForestryConfig.CLIENT.enableHints.get()) {
			if (!hints.isEmpty()) {
                this.ledgerManager.add(new HintLedger(this.ledgerManager, hints));
			}
		}
	}

	protected final void addOwnerLedger(IOwnedTile ownedTile) {
        this.ledgerManager.add(new OwnerLedger(this.ledgerManager, ownedTile));
	}

	@Override
	public void onClose() {
		super.onClose();
        this.ledgerManager.onClose();
	}

	public ColourProperties getFontColor() {
		return ColourProperties.INSTANCE;
	}

	public Font font() {
		return this.font;
	}

	@Override
	public int getGuiLeft() {
		return this.leftPos;
	}

	@Override
	public int getGuiTop() {
		return this.topPos;
	}

	public Minecraft getMinecraft() {
		return getGameInstance();
	}

	@Nullable private MouseButtonEvent mouseEvent;
	private boolean doubleClick;

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		this.mouseEvent = event;
		this.doubleClick = doubleClick;
		try { return mouseClicked(event.x(), event.y(), event.button()); }
		finally { this.mouseEvent = null; this.doubleClick = false; }
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.mouseEvent = event;
		try { return mouseReleased(event.x(), event.y(), event.button()); }
		finally { this.mouseEvent = null; }
	}

	/** Compatibility hook for Forestry screens while their widget input is migrated. */
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		// / Handle ledger clicks
		this.ledgerManager.handleMouseClicked(mouseX, mouseY, mouseButton);
		this.widgetManager.handleMouseClicked(mouseX, mouseY, mouseButton);
		return super.mouseClicked(this.mouseEvent != null ? this.mouseEvent : new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(mouseButton, 0)), this.doubleClick);
	}

	/** Compatibility hook preserving existing screen-specific release handling. */
	public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
		if (this.widgetManager.handleMouseRelease(mouseX, mouseY, mouseButton)) {
			return true;
		}
		return super.mouseReleased(this.mouseEvent != null ? this.mouseEvent : new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(mouseButton, 0)));
	}

	@Nullable
	public TankWidget getTankAtPosition(double mouseX, double mouseY) {
		for (Widget widget : this.widgetManager.getWidgets()) {
			if (widget instanceof TankWidget tankWidget && widget.isMouseOver(mouseX - this.leftPos, mouseY - this.topPos)) {
				return tankWidget;
			}
		}
		return null;
	}

	@Nullable
	protected Slot getSlotAtPosition(double mouseX, double mouseY) {
		for (int k = 0; k < this.menu.slots.size(); ++k) {
			Slot slot = this.menu.slots.get(k);

			if (isMouseOverSlot(slot, mouseX, mouseY)) {
				return slot;
			}
		}

		return null;
	}

	private boolean isMouseOverSlot(Slot par1Slot, double mouseX, double mouseY) {
		return isHovering(par1Slot.x, par1Slot.y, 16, 16, mouseX, mouseY);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.ledgerManager.drawTooltips(graphics, mouseX, mouseY);

		if (this.menu.getCarried().isEmpty()) {
			GuiUtil.drawToolTips(graphics, this, this.widgetManager.getWidgets(), mouseX, mouseY);
			GuiUtil.drawToolTips(graphics, this, children(), mouseX, mouseY);
			GuiUtil.drawToolTips(graphics, this, this.menu.slots, mouseX, mouseY);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);
		drawBackground(graphics);

        this.widgetManager.updateWidgets(mouseX - this.leftPos, mouseY - this.topPos);

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(this.leftPos, this.topPos);
		drawWidgets(graphics);
		pose.popMatrix();
	}

	protected void drawBackground(GuiGraphicsExtractor graphics) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
	}

	protected void drawWidgets(GuiGraphicsExtractor graphics) {
        this.ledgerManager.drawLedgers(graphics);
        this.widgetManager.drawWidgets(graphics);
	}

	@Override
	public int getSizeX() {
		return this.imageWidth;
	}

	@Override
	public int getSizeY() {
		return this.imageHeight;
	}

	@Override
	public Minecraft getGameInstance() {
		return Preconditions.checkNotNull(this.minecraft);
	}

	public List<Rect2i> getExtraGuiAreas() {
		return this.ledgerManager.getLedgerAreas();
	}

	public TextLayoutHelper getTextLayout() {
		return this.textLayout;
	}

	private static LinkedListMultimap<String, String> readDefaultHints() {
		LinkedListMultimap<String, String> map = LinkedListMultimap.create();
		Properties prop = new Properties();

		try {
			InputStream hintStream = GuiForestry.class.getResourceAsStream("/config/forestry/hints.properties");
			prop.load(hintStream);
		} catch (IOException | NullPointerException e) {
			Forestry.LOGGER.error("Failed to load hints file.", e);
		}

		for (String key : prop.stringPropertyNames()) {
			String list = prop.getProperty(key);

			if (!list.isEmpty()) {
				for (String parsedHint : list.split(";+")) {
					map.put(key, parsedHint);
				}
			}
		}

		return map;
	}
}
