package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.TileFermenter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiFermenter extends GuiForestryTitled<ContainerFermenter> {
	private final TileFermenter tile;

	public GuiFermenter(ContainerFermenter container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/fermenter.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 35, 19, 0));
        this.widgetManager.add(new TankWidget(this.widgetManager, 125, 19, 1));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mX, int mY, float partialTicks) {
		super.extractBackground(graphics, mX, mY, partialTicks);

		// Fuel remaining
		int fuelRemain = this.tile.getBurnTimeRemainingScaled(16);
		if (fuelRemain > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 98, this.topPos + 46 + 17 - fuelRemain, 176, 78 + 17 - fuelRemain, 4, fuelRemain, 256, 256);
		}

		// Raw bio mush remaining
		int bioRemain = this.tile.getFermentationProgressScaled(16);
		if (bioRemain > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 74, this.topPos + 32 + 17 - bioRemain, 176, 60 + 17 - bioRemain, 4, bioRemain, 256, 256);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("fermenter");
		addPowerLedger(this.tile.getEnergyManager());
	}
}
