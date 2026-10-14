package forestry.core.content.energy.screen;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.widgets.SocketWidget;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.energy.menu.BiogasEngineMenu;
import forestry.core.content.energy.tiles.BiogasEngineBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BiogasEngineScreen extends EngineScreen<BiogasEngineMenu, BiogasEngineBlockEntity> {
	public BiogasEngineScreen(BiogasEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/bioengine.png", menu, inv, title, menu.getTile());

        this.widgetManager.add(new TankWidget(this.widgetManager, 89, 19, 0));
        this.widgetManager.add(new TankWidget(this.widgetManager, 107, 19, 1));

        this.widgetManager.add(new BiogasSlot(this.widgetManager, 30, 47, 2));

		this.widgetManager.add(new SocketWidget(this.widgetManager, 60, 47, menu.getTile(), 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		int temperature = this.engine.getOperatingTemperatureScaled(16);
		if (temperature > 16) {
			temperature = 16;
		}
		if (temperature > 0) {
			graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 53, this.topPos + 47 + 16 - temperature, 176, 60 + 16 - temperature, 4, temperature, 256, 256);
		}
	}
}
