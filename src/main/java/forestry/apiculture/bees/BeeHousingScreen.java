package forestry.apiculture.bees;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.render.EnumTankLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BeeHousingScreen extends GuiForestryTitled<BeeHousingMenu> {
	private final IGuiBeeHousingDelegate delegate;

	public enum Icon {
		APIARY("/apiary.png"),
		BEE_HOUSE("/alveary.png");

		public static final Icon[] VALUES = values();
		private final String path;

		Icon(String path) {
			this.path = path;
		}
	}

	public BeeHousingScreen(BeeHousingMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + container.getIcon().path, container, inv, title, 176, 190);
		this.delegate = container.getDelegate();

	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		drawHealthMeter(graphics, this.leftPos + 20, this.topPos + 37, this.delegate.getHealthScaled(46), EnumTankLevel.rateTankLevel(this.delegate.getHealthScaled(100)));
	}

	private void drawHealthMeter(GuiGraphicsExtractor graphics, int x, int y, int height, EnumTankLevel rated) {
		int i = 176 + rated.getLevelScaled(16);
		int k = 0;

		graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, x, y + 46 - height, i, k + 46 - height, 4, height, 256, 256);
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.delegate);
		addClimateLedger(this.delegate);
		addHintLedger(this.delegate.getHintKey());
		addOwnerLedger(this.delegate);
	}
}
