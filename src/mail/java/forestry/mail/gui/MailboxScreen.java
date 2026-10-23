package forestry.mail.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestry;
import forestry.mail.postoffice.MailboxBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MailboxScreen extends GuiForestry<MailboxMenu> {
	private final MailboxBlockEntity tile;

	public MailboxScreen(MailboxMenu container, Inventory inv, Component title) {
		super(Constants.TEXTURE_PATH_GUI + "/mailbox.png", container, inv, title, 230, 227);
		this.tile = container.getTile();
	}

	@Override
	protected void addLedgers() {
		addErrorLedger(this.tile);
		addHintLedger("mailbox");
	}
}
