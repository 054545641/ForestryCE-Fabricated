package forestry.core.content.machines.gui;

import forestry.core.content.machines.tiles.TileSmelter;
import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.SocketWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiSmelter extends GuiForestryTitled<ContainerSmelter> {
	private final TileSmelter tile;

	public GuiSmelter(ContainerSmelter container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/smelter.png", container, inventory, title);
		this.tile = container.getTile();
		this.widgetManager.add(new SocketWidget(this.widgetManager, 95, 21, this.tile, 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		// Smelting progress bar
		int progress = this.tile.getProgressScaled(50);
		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 81, this.topPos + 39, // where to draw thing
			176, 52, // coordinates of thing to be drawn
			progress, 16, 256, 256); // width and height of thing to be drawn

		// Status indicator. Kinda redundant but it's a nice visual indicator
		if (!this.tile.getErrorLogic().hasErrors()) {
			graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 96, this.topPos + 58, 176, 68, 14, 14, 256, 256);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("smelter");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
