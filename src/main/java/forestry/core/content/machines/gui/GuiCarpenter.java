package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.TileCarpenter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiCarpenter extends GuiForestryTitled<ContainerCarpenter> {
	private final TileCarpenter tile;

	public GuiCarpenter(ContainerCarpenter container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/carpenter.png", container, inventory, title, 176, 218);

		this.tile = container.getTile();
		this.widgetManager.add(new TankWidget(this.widgetManager, 150, 17, 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);
		int progressScaled = this.tile.getProgressScaled(16);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 98, this.topPos + 51 + 16 - progressScaled, 176, 60 + 16 - progressScaled, 4, progressScaled, 256, 256);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addPowerLedger(this.tile.getEnergyManager());
		addHintLedger("carpenter");
	}
}
