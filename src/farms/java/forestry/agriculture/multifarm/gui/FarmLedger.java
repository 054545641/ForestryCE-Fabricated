package forestry.agriculture.multifarm.gui;

import forestry.core.platform.gui.ledgers.Ledger;
import forestry.core.platform.gui.ledgers.LedgerManager;
import forestry.core.platform.util.ResourceUtil;
import forestry.core.platform.util.StringUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class FarmLedger extends Ledger {
	private final IFarmLedgerDelegate delegate;

	public FarmLedger(LedgerManager ledgerManager, IFarmLedgerDelegate delegate) {
		super(ledgerManager, "farm");
		this.delegate = delegate;

		int titleHeight = StringUtil.getLineHeight(this.maxTextWidth, getTooltip());
		this.maxHeight = titleHeight + 110;
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int y, int x) {

		// Draw background
		drawBackground(graphics, y, x);
		y += 4;

		int xIcon = x + 3;
		int xBody = x + 10;
		int xHeader = x + 22;

		// Draw icon
		TextureAtlasSprite textureAtlasSprite = ResourceUtil.getSprite(TextureAtlas.LOCATION_ITEMS, Identifier.withDefaultNamespace("item/water_bucket"));
		drawSprite(graphics, textureAtlasSprite, xIcon, y);
		y += 4;

		if (!isFullyOpened()) {
			return;
		}

		y += drawHeader(graphics, Component.translatable("for.gui.hydration"), xHeader, y);
		y += 4;

		y += drawSubheader(graphics, Component.translatable("for.gui.hydr.heat").append(":"), xBody, y);
		y += 3;
		y += drawText(graphics, StringUtil.floatAsPercent(this.delegate.getHydrationTempModifier()), xBody, y);
		y += 3;

		y += drawSubheader(graphics, Component.translatable("for.gui.hydr.humid").append(":"), xBody, y);
		y += 3;
		y += drawText(graphics, StringUtil.floatAsPercent(this.delegate.getHydrationHumidModifier()), xBody, y);
		y += 3;

		y += drawSubheader(graphics, Component.translatable("for.gui.hydr.rainfall").append(":"), xBody, y);
		y += 3;
		y += drawText(graphics, StringUtil.floatAsPercent(this.delegate.getHydrationRainfallModifier()) + " (" + this.delegate.getDrought() + " d)", xBody, y);
		y += 3;

		y += drawSubheader(graphics, Component.translatable("for.gui.hydr.overall").append(":"), xBody, y);
		y += 3;
		drawText(graphics, StringUtil.floatAsPercent(this.delegate.getHydrationModifier()), xBody, y);
	}

	@Override
	public Component getTooltip() {
		float hydrationModifier = this.delegate.getHydrationModifier();
		return Component.literal(StringUtil.floatAsPercent(hydrationModifier) + ' ')
			.append(Component.translatable("for.gui.hydration"));
	}
}
