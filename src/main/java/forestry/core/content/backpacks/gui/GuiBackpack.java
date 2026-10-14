package forestry.core.content.backpacks.gui;

import forestry.core.platform.config.Constants;
import forestry.core.platform.gui.GuiForestry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuiBackpack extends GuiForestry<ContainerBackpack> {
	public GuiBackpack(ContainerBackpack container, Inventory inv, Component title) {
		super(getTextureString(container), container, inv, title, 176, container.getSize() == ContainerBackpack.Size.T2 ? 192 : 166);
	}

	private static String getTextureString(ContainerBackpack container) {
		ContainerBackpack.Size size = container.getSize();
		if (size == ContainerBackpack.Size.T2) {
			return Constants.TEXTURE_PATH_GUI + "/backpack_t2.png";
		}
		return Constants.TEXTURE_PATH_GUI + "/backpack.png";
	}

	@Override
	protected void addLedgers() {

	}
}
