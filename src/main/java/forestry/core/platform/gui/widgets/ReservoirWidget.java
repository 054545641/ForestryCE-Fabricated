package forestry.core.platform.gui.widgets;

public class ReservoirWidget extends TankWidget {
	public ReservoirWidget(WidgetManager manager, int xPos, int yPos, int slot) {
		super(manager, xPos, yPos, slot);
		this.height = 16;
		this.drawOverlay = false;
	}
}
