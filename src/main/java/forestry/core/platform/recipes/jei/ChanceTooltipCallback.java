package forestry.core.platform.recipes.jei;

import forestry.core.platform.util.JeiUtil;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;

public class ChanceTooltipCallback implements IRecipeSlotRichTooltipCallback {
	private final float chance;

	public ChanceTooltipCallback(float chance) {
		if (chance < 0) {
			chance = 0;
		} else if (chance > 1.0) {
			chance = 1.0f;
		}
		this.chance = chance;
	}

	@Override
	public void onRichTooltip(IRecipeSlotView recipeSlotView, ITooltipBuilder tooltip) {
		tooltip.add(JeiUtil.formatChance(chance));
	}
}
