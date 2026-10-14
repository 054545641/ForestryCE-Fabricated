package forestry.core.content.sorting.gui.widgets;

import forestry.api.ForestryConstants;
import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.core.platform.gui.widgets.WidgetScrollBar;
import forestry.core.content.sorting.gui.GuiGeneticFilter;
import forestry.core.content.sorting.gui.ISelectableProvider;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public class SelectionWidget extends Widget {
	public static final Identifier TEXTURE = ForestryConstants.forestry(Constants.TEXTURE_PATH_GUI + "/filter_selection.png");
	final WidgetScrollBar scrollBar;
	@Nullable
	private SelectionLogic<?> logic;
	final GuiGeneticFilter gui;

	public SelectionWidget(WidgetManager manager, int xPos, int yPos, WidgetScrollBar scrollBar, GuiGeneticFilter gui) {
		super(manager, xPos, yPos);
		this.width = 212;
		this.height = 88;
		this.scrollBar = scrollBar;
		this.gui = gui;
	}

	public <S> void setProvider(@Nullable ISelectableProvider<S> provider) {
		if (provider == null) {
            this.logic = null;
		} else {
			this.logic = new SelectionLogic<>(this, provider);
		}
	}

	public boolean isSame(ISelectableProvider<?> provider) {
		return this.logic != null && this.logic.isSame(provider);
	}

	@Nullable
	public SelectionLogic<?> getLogic() {
		return this.logic;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		if (this.logic == null) {
			return;
		}

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, TEXTURE, startX + this.xPos, startY + this.yPos, 0, 0, this.width, this.height, 256, 256);
        this.logic.draw(graphics);

		graphics.text(this.gui.font(), Component.translatable("for.gui.filter.seletion"), startX + this.xPos + 12, startY + this.yPos + 4, 0xFF000000 | (this.manager.gui.getFontColor().get("gui.title")));
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.logic != null && super.isMouseOver(mouseX, mouseY);
	}

	@Nullable
	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		if (this.logic == null) {
			return null;
		}
		return this.logic.getToolTip(mouseX, mouseY);
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		if (this.logic == null) {
			return;
		}
        this.logic.select(mouseX, mouseY);
	}

	public void filterEntries(String filter) {
		if (this.logic == null) {
			return;
		}
        this.logic.filterEntries(filter);
	}
}
