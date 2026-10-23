package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.TileBottler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiBottler extends GuiForestryTitled<ContainerBottler> {
	private final TileBottler tile;

	public GuiBottler(ContainerBottler container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/bottler.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 80, 14, 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		TileBottler bottler = this.tile;
		int progressArrow = bottler.getProgressScaled(22);
		if (progressArrow > 0) {
			if (bottler.isFillRecipe) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 108, this.topPos + 35, 177, 74, progressArrow, 16, 256, 256);
			} else {
				graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 46, this.topPos + 35, 177, 74, progressArrow, 16, 256, 256);
			}
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("bottler");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
