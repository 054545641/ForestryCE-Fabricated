package forestry.mail.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestry;
import forestry.mail.postoffice.StampCollectorBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class StampCollectorScreen extends GuiForestry<StampCollectorMenu> {
	private final StampCollectorBlockEntity tile;

	public StampCollectorScreen(StampCollectorMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/stamp_collector.png", container, inv, title, 176, 193);
		this.tile = container.getTile();
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("philatelist");
	}
}
