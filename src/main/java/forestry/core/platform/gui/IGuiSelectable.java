package forestry.core.platform.gui;

import net.minecraft.server.level.ServerPlayer;

public interface IGuiSelectable {
	// server
	void handleSelectionRequest(ServerPlayer player, int primary, int secondary);
}
