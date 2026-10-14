package forestry.core.content.analyzer;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.platform.render.EnumTankLevel;
import forestry.core.content.analyzer.TileAnalyzer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import forestry.core.platform.gui.GuiForestryTitled;

// The block form of the analyzer
public class GuiAnalyzer extends GuiForestryTitled<ContainerAnalyzer> {
	private final TileAnalyzer tile;

	public GuiAnalyzer(ContainerAnalyzer analyzer, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/alyzer.png", analyzer, inventory, title, 176, 176);
		this.tile = analyzer.getTile();

		this.widgetManager.add(new TankWidget(this.widgetManager, 95, 24, 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseY, int mouseX, float partialTicks) {
		super.extractBackground(graphics, mouseY, mouseX, partialTicks);
		drawAnalyzeMeter(graphics, this.leftPos + 64, this.topPos + 30, this.tile.getProgressScaled(46), EnumTankLevel.rateTankLevel(this.tile.getProgressScaled(100)));
	}

	private void drawAnalyzeMeter(GuiGraphicsExtractor graphics, int x, int y, int height, EnumTankLevel rated) {
		int i = 176 + rated.getLevelScaled(16);
		int k = 60;

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, x, y + 46 - height, i, k + 46 - height, 4, height, 256, 256);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("analyzer");
	}
}
