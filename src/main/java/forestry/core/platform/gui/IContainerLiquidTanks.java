package forestry.core.platform.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IContainerLiquidTanks extends IContainerTank {
	void handlePipetteClickClient(int slot, Player player);

	void handlePipetteClick(int slot, ServerPlayer player);
}
