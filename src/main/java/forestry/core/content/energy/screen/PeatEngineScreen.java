package forestry.core.content.energy.screen;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.widgets.SocketWidget;
import forestry.core.content.energy.menu.PeatEngineMenu;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PeatEngineScreen extends EngineScreen<PeatEngineMenu, PeatEngineBlockEntity> {
	public PeatEngineScreen(PeatEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/peatengine.png", menu, inv, title, menu.getTile());
		this.widgetManager.add(new SocketWidget(this.widgetManager, 71, 44, menu.getTile(), 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		if (this.engine.isBurning()) {
			int progress = this.engine.getBurnTimeRemainingScaled(12);
			graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 45, this.topPos + 27 + 12 - progress, 176, 12 - progress, 14, progress + 2, 256, 256);
		}
	}
}
