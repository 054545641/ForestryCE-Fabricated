package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.TileStill;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiStill extends GuiForestryTitled<ContainerStill> {
	private final TileStill tile;

	public GuiStill(ContainerStill container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/still.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 35, 15, 0));
        this.widgetManager.add(new TankWidget(this.widgetManager, 125, 15, 1));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 81, this.topPos + 57, 176, 60, 14, 14, 256, 256);

		if (this.tile.getWorkCounter() > 0) {
			int massRemaining = this.tile.getProgressScaled(16);
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 84, this.topPos + 17 + massRemaining, 176, 74 + massRemaining, 4, 17 - massRemaining, 256, 256);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("still");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
