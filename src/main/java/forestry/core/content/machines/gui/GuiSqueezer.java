package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.SocketWidget;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.machines.tiles.TileSqueezer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiSqueezer extends GuiForestryTitled<ContainerSqueezer> {
	private final TileSqueezer tile;

	public GuiSqueezer(ContainerSqueezer container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/squeezersocket.png", container, inventory, title);
		this.tile = container.getTile();
        this.widgetManager.add(new TankWidget(this.widgetManager, 122, 18, 0));
        this.widgetManager.add(new SocketWidget(this.widgetManager, 75, 20, this.tile, 0));
	}

	@Override
	protected void drawWidgets(GuiGraphicsExtractor graphics) {
		//TODO: Make this more consistent
		int progress = this.tile.getProgressScaled(43);
		graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, 75, 41, 176, 60, progress, 18, 256, 256);

		super.drawWidgets(graphics);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addPowerLedger(this.tile.getEnergyManager());
		addHintLedger("squeezer");
	}
}
