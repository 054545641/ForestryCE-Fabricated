package forestry.core.content.worktable.screens.widgets;

import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.core.platform.util.SoundUtil;
import forestry.core.content.worktable.screens.WorktableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ClearWorktable extends Widget {
	public ClearWorktable(WidgetManager manager, int xPos, int yPos) {
		super(manager, xPos, yPos);
        this.width = 7;
        this.height = 7;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		WorktableMenu.clearRecipe();
		SoundUtil.playButtonClick();
	}
}
