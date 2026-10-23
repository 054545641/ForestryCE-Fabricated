package forestry.core.manual;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

public interface IManualAccess {
	boolean isAvailable();

	// only called when isAvailable, so implementations may touch their provider's classes
	void openManual(ServerPlayer player, Item manualItem);
}
