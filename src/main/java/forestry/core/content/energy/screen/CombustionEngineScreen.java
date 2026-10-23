package forestry.core.content.energy.screen;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.widgets.SocketWidget;
import forestry.core.platform.gui.widgets.TankWidget;
import forestry.core.content.energy.menu.CombustionEngineMenu;
import forestry.core.content.energy.tiles.CombustionEngineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CombustionEngineScreen extends EngineScreen<CombustionEngineMenu, CombustionEngineBlockEntity> {
	public CombustionEngineScreen(CombustionEngineMenu menu, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/fuelengine.png", menu, inv, title, menu.getTile());

		this.widgetManager.add(new TankWidget(this.widgetManager, 53, 19, 0));
		this.widgetManager.add(new TankWidget(this.widgetManager, 107, 19, 1));

		this.widgetManager.add(new BiogasSlot(this.widgetManager, 80, 53, 2));
		this.widgetManager.add(new BiogasSlot(this.widgetManager, 80, 27, 3));

		this.widgetManager.add(new SocketWidget(this.widgetManager, 26, 40, menu.getTile(), 0));
	}
}
