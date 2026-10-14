package forestry.core.content.sorting.gui;

import forestry.api.core.genetics.filter.IFilterLogic;
import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.Drawable;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.gui.widgets.WidgetScrollBar;
import forestry.core.content.sorting.gui.widgets.RuleWidget;
import forestry.core.content.sorting.gui.widgets.SelectionWidget;
import forestry.core.content.sorting.gui.widgets.SpeciesWidget;
import forestry.core.content.sorting.tiles.TileGeneticFilter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

public class GuiGeneticFilter extends GuiForestryTitled<ContainerGeneticFilter> {
	private final TileGeneticFilter tile;
	private final WidgetScrollBar scrollBar;
	public final SelectionWidget selection;
	@Nullable
	private EditBox searchField;

	public GuiGeneticFilter(ContainerGeneticFilter container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/filter.png", container, inventory, title, 212, 222);

		this.tile = container.getTile();

		for (int i = 0; i < 6; i++) {
			Direction facing = Direction.from3DDataValue(i);
            this.widgetManager.add(new RuleWidget(this.widgetManager, 8 + 36, 18 + i * 18, facing, this));
		}

		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 3; j++) {
				for (int k = 0; k < 2; k++) {
                    this.widgetManager.add(new SpeciesWidget(this.widgetManager, 44 + 36 + j * 45 + k * 18, 18 + i * 18, Direction.from3DDataValue(i), j, k == 0, this));
				}
			}
		}

		this.scrollBar = new WidgetScrollBar(this.widgetManager, 157 + 36, 150, 12, 64, new Drawable(Identifier.withDefaultNamespace("container/creative_inventory/scroller"), 0, 0, 12, 15) {
			@Override public void draw(GuiGraphicsExtractor graphics, int y, int width, int height, int x) {
				graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureLocation, x, y, width, height);
			}
		});
        this.widgetManager.add(this.selection = new SelectionWidget(this.widgetManager, 0, 134, this.scrollBar, this));
        this.widgetManager.add(this.scrollBar);
        this.scrollBar.setVisible(false);
	}

	public <S> void onModuleClick(ISelectableProvider<S> provider) {
		if (this.selection.isSame(provider)) {
			deselectFilter();
		} else {
			selectFilter(provider);
		}
	}

	private <S> void selectFilter(ISelectableProvider<S> provider) {
        this.selection.setProvider(provider);
		if (this.searchField != null) {
            this.searchField.setEditable(true);
            this.searchField.setVisible(true);
		}
        this.selection.filterEntries(this.searchField != null ? this.searchField.getValue() : "");
		for (Slot slot : this.menu.slots) {
			if (slot instanceof SlotGeneticFilter filter) {
				filter.setEnabled(false);
			}
		}
	}

	private void deselectFilter() {
		this.selection.setProvider(null);
		if (this.searchField != null) {
            this.searchField.setEditable(false);
            this.searchField.setVisible(false);
			setFocused(null);
		}
        this.scrollBar.setVisible(false);
		for (Slot slot : this.menu.slots) {
			if (slot instanceof SlotGeneticFilter filter) {
				filter.setEnabled(true);
			}
		}
	}

	@Override
	public void init() {
		super.init();

		String oldString = this.searchField != null ? this.searchField.getValue() : "";

		this.searchField = new forestry.core.platform.gui.FilterSearchField(this.font, this.leftPos + this.selection.getX() + 89 + 36, this.selection.getY() + this.topPos + 4, 80, this.font.lineHeight);
		this.searchField.setValue(oldString);
		this.searchField.setResponder(value -> {
			this.scrollBar.setValue(0);
			this.selection.filterEntries(value);
		});
		this.searchField.setVisible(this.selection.getLogic() != null);
		this.searchField.setEditable(this.selection.getLogic() != null);
		addWidget(this.searchField);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		if (this.searchField != null) {
			this.searchField.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		}
	}

	@Nullable
	@Override
	protected Slot getSlotAtPosition(double mouseX, double mouseY) {
		Slot slot = super.getSlotAtPosition(mouseX, mouseY);
		if (slot instanceof SlotGeneticFilter && this.selection.getLogic() != null) {
			return null;
		}
		return slot;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		// Native container screens now consume ordinary clicks too. Still run deselection
		// after dispatch, otherwise clicking outside the selector leaves inventory slots disabled.
		super.mouseClicked(mouseX, mouseY, mouseButton);

		Widget widget = this.widgetManager.getAtPosition(mouseX - this.leftPos, mouseY - this.topPos);
		if (widget == null) {
			deselectFilter();
		}
		return true;
	}

	@Override
	protected void addLedgers() {
		addHintLedger("filter");
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && this.scrollBar.isVisible()
			&& this.selection.isMouseOver(mouseX - this.leftPos, mouseY - this.topPos)) {
			this.scrollBar.setValue(this.scrollBar.getValue() - (int) Math.signum(scrollY));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	public IFilterLogic getLogic() {
		return this.tile.getLogic();
	}
}
