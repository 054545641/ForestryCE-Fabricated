package forestry.agriculture.multifarm.gui;

import forestry.api.core.tooltips.ToolTip;
import forestry.api.agriculture.IFarmLogic;
import forestry.api.agriculture.IFarmType;
import forestry.core.platform.gui.GuiUtil;
import forestry.core.platform.gui.widgets.Widget;
import forestry.core.platform.gui.widgets.WidgetManager;
import forestry.agriculture.multifarm.multiblock.IFarmControllerInternal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MultifarmLogicSlot extends Widget {
	private final IFarmControllerInternal farmController;
	private final Direction farmDirection;

	public MultifarmLogicSlot(IFarmControllerInternal farmController, WidgetManager manager, int xPos, int yPos, Direction farmDirection) {
		super(manager, xPos, yPos);
		this.farmController = farmController;
		this.farmDirection = farmDirection;
	}

	private IFarmLogic getLogic() {
		return this.farmController.getFarmLogic(this.farmDirection);
	}

	private IFarmType getProperties() {
		return getLogic().getType();
	}

	private ItemStack getStackIndex() {
		return getProperties().getIcon();
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int startX, int startY) {
		if (!getStackIndex().isEmpty()) {
			Minecraft minecraft = Minecraft.getInstance();

			GuiUtil.drawItemStack(graphics, minecraft.font, getStackIndex(), startX + this.xPos, startY + this.yPos);
		}
	}

	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		if (isMouseOver(mouseX, mouseY)) {
			return this.toolTip;
		} else {
			return null;
		}
	}

	protected final ToolTip toolTip = new ToolTip(250) {
		@Override
		public void refresh() {
            MultifarmLogicSlot.this.toolTip.clear();
            MultifarmLogicSlot.this.toolTip.add(getProperties().getDisplayName(getLogic().isManual()));
            MultifarmLogicSlot.this.toolTip.add(Component.translatable("for.gui.farm.fertilizer", getProperties().getFertilizerConsumption(MultifarmLogicSlot.this.farmController)));
            MultifarmLogicSlot.this.toolTip.add(Component.translatable("for.gui.farm.water", getProperties().getWaterConsumption(MultifarmLogicSlot.this.farmController, MultifarmLogicSlot.this.farmController.getFarmLedgerDelegate().getHydrationModifier())));
		}
	};
}
