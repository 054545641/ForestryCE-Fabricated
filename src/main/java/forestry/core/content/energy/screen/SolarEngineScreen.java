package forestry.core.content.energy.screen;

import forestry.core.platform.config.Constants;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.render.ColourProperties;
import forestry.core.content.energy.menu.SolarEngineMenu;
import forestry.core.content.energy.tiles.SolarEngineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

public class SolarEngineScreen extends EngineScreen<SolarEngineMenu, SolarEngineBlockEntity> {
	private static final int SUN_U = 176;
	private static final int SUN_V = 0;
	private static final int SUN_SIZE = 16;

	private static final int TEXT_X = 44;
	private static final int TEXT_TOP = 32;
	private static final int TEXT_LINE_HEIGHT = 10;

	public SolarEngineScreen(SolarEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/solarengine.png", menu, inv, title, menu.getTile());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);

		int active = this.engine.getClientActivePanelCount();
		int total = this.engine.getClientPanelCount();
		int textColour = ColourProperties.INSTANCE.get("gui.screen");
		// how much of a panel's rated output the current sky is delivering.
		double insolation = SolarEngineBlockEntity.insolation(this.engine.getLevel(), this.engine.getClientSkyDarken()) * 100.0;
		// how much the size of the array multiplies that output by.
		double multiplier = SolarEngineBlockEntity.calculateMult(this.engine.getLevel(), active);
		// with the bonus turned off the multiplier is always 1, so the row says nothing worth a line
		boolean showEfficiency = ForestryConfig.SERVER.solarArrayBonusFactor.get() > 0.0;
		// recenter the two remaining rows in the panel when the efficiency row is hidden
		int offset = showEfficiency ? 0 : 6;

		drawRow(graphics, 0, offset, Component.translatable("for.gui.engine.tin.array_size", arraySize(active, total)), textColour);

		drawRow(graphics, 1, offset, Component.translatable("for.gui.engine.tin.insolation",
			Component.literal(formatPercent(insolation)).withStyle(percentColour(insolation))), textColour);

		if (showEfficiency) {
			drawRow(graphics, 2, offset, Component.translatable("for.gui.engine.tin.efficiency",
				Component.literal(String.format(Locale.ROOT, "%.2f", multiplier)).withStyle(multiplier > 1.0 ? ChatFormatting.GREEN : ChatFormatting.GOLD)), textColour);
		}

		if (active > 0) {
			// show a sun when we have insolation, a moon when we don't
			int decalU = insolation > 0.0 ? SUN_U : SUN_U + 16;
			graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.textureFile, this.leftPos + 14, this.topPos + 38, decalU, SUN_V, SUN_SIZE, SUN_SIZE, 256, 256);
		}
	}

	private void drawRow(GuiGraphicsExtractor graphics, int row, int offset, Component text, int colour) {
		graphics.text(this.font, text, this.leftPos + TEXT_X, this.topPos + TEXT_TOP + offset + row * TEXT_LINE_HEIGHT, 0xFF000000 | (colour), false);
	}

	private static Component arraySize(int active, int total) {
		if (active >= total) {
			return Component.literal(Integer.toString(total)).withStyle(ChatFormatting.GREEN);
		}
		return Component.literal(active + "/" + total).withStyle(active == 0 ? ChatFormatting.DARK_RED : ChatFormatting.GOLD);
	}

	private static String formatPercent(double percent) {
		String formatted = String.format(Locale.ROOT, "%.2f", percent);
		if (formatted.indexOf('.') >= 0) {
			formatted = formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return formatted + "%";
	}

	private static ChatFormatting percentColour(double percent) {
		if (percent > 100.0) {
			return ChatFormatting.BLUE;
		}
		if (percent == 100.0) {
			return ChatFormatting.GREEN;
		}
		if (percent <= 0.0) {
			return ChatFormatting.DARK_RED;
		}
		return ChatFormatting.GOLD;
	}
}
