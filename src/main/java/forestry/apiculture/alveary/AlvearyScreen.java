package forestry.apiculture.alveary;

import forestry.apiculture.alveary.multiblock.IAlvearyControllerInternal;
import forestry.apiculture.alveary.multiblock.AbstractAlvearyBlockEntity;
import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.render.EnumTankLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AlvearyScreen extends GuiForestryTitled<AlvearyMenu> {
	private final AbstractAlvearyBlockEntity tile;

	public AlvearyScreen(AlvearyMenu container, Inventory inventory, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/alveary.png", container, inventory, title, 176, 190);
		this.tile = container.getTile();

	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		IAlvearyControllerInternal alvearyController = this.tile.getMultiblockLogic().getController();
		drawHealthMeter(graphics, this.leftPos + 20, this.topPos + 37, alvearyController.getHealthScaled(46), EnumTankLevel.rateTankLevel(alvearyController.getHealthScaled(100)));
	}

	private void drawHealthMeter(GuiGraphicsExtractor graphics, int x, int y, int height, EnumTankLevel rated) {
		int i = 176 + rated.getLevelScaled(16);
		int k = 0;

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, x, y + 46 - height, i, k + 46 - height, 4, height, 256, 256);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addClimateLedger(this.tile);
		addHintLedger("apiary");
		addOwnerLedger(this.tile);
	}
}
