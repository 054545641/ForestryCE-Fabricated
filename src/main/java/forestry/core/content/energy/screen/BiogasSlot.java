package forestry.core.content.energy.screen;


import forestry.api.core.tooltips.ToolTip;
import forestry.core.platform.gui.widgets.ReservoirWidget;
import forestry.core.platform.gui.widgets.WidgetManager;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.FluidStack;
import forestry.core.platform.fluids.StandardTank;

public class BiogasSlot extends ReservoirWidget {
	public BiogasSlot(WidgetManager manager, int xPos, int yPos, int slot) {
		super(manager, xPos, yPos, slot);
	}

	@Override
	public ToolTip getToolTip(int mouseX, int mouseY) {
		ToolTip toolTip = new ToolTip();
		StandardTank tank = getTank();
		if (tank != null) {
			FluidStack fluid = tank.getFluid();
			if (fluid.isEmpty()) {
				toolTip.add(Component.translatable("for.gui.empty"));
			} else {
				toolTip.add(fluid.getHoverName());
			}
		}
		return toolTip;
	}

	@Override
	public void handleMouseClick(double mouseX, double mouseY, int mouseButton) {
		// do not allow pipette
	}
}
