package forestry.core.content.machines.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestryTitled;
import forestry.core.platform.gui.widgets.ReservoirWidget;
import forestry.core.content.machines.tiles.TileFabricator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class GuiFabricator extends GuiForestryTitled<ContainerFabricator> {
	private final TileFabricator tile;

	public GuiFabricator(ContainerFabricator container, Inventory player, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/thermionic_fabricator.png", container, player, title, 176, 211);

		this.tile = container.getTile();
		this.widgetManager.add(new ReservoirWidget(this.widgetManager, 26, 48, 0));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		int heatScaled = this.tile.getHeatScaled(52);
		if (heatScaled > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 55, this.topPos + 17 + 52 - heatScaled, 192, 52 - heatScaled, 4, heatScaled, 256, 256);
		}

		int meltingPointScaled = this.tile.getMeltingPointScaled(52);
		if (meltingPointScaled > 0) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 52, this.topPos + 15 + 52 - meltingPointScaled, 196, 0, 10, 5, 256, 256);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

		boolean mouseOverHeatingBar = (
			mouseX >= this.leftPos + 55
				&& mouseX <= this.leftPos + 59
				&& mouseY >= this.topPos + 16
				&& mouseY <= this.topPos + 70
		);

		if (mouseOverHeatingBar) {
			List<Component> messages;
			if (this.tile.getMeltingPoint() > 0) {
				messages = List.of(
					Component.translatable("for.gui.fabricator.heat", this.tile.getHeat(), TileFabricator.MAX_HEAT),
					Component.translatable("for.gui.fabricator.requiredHeat", this.tile.getMeltingPoint()));
			} else {
				messages = List.of(Component.translatable("for.gui.fabricator.heat", this.tile.getHeat(), TileFabricator.MAX_HEAT));
			}

			guiGraphics.setTooltipForNextFrame(this.font, messages, Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addPowerLedger(this.tile.getEnergyManager());
		addHintLedger("fabricator");
	}
}
