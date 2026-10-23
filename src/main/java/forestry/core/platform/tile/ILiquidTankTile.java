package forestry.core.platform.tile;

import forestry.api.core.ILocationProvider;
import forestry.core.platform.fluids.ITankManager;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.network.packets.PacketTankLevelUpdate;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.server.level.ServerPlayer;
import forestry.api.core.fluids.FluidVolume;

public interface ILiquidTankTile extends ILocationProvider, TankManager.Listener {
	ITankManager getTankManager();

	@Override
	default void updateTankLevels(StandardTank tank) {
		var level = getLevel();
		if (level == null || level.isClientSide()) return;
		var tile = level.getBlockEntity(getBlockPos());
		if (tile != null) tile.setChanged();
		if (this instanceof IRenderableTile) {
			NetworkUtil.sendNetworkPacket(new PacketTankLevelUpdate(this, tank.getTankIndex(), tank.getFluid()), getBlockPos(), level);
		}
	}

	@Override
	default void sendTankUpdate(ServerPlayer player, int tankIndex, FluidVolume fluid, long capacity) {
		NetworkUtil.sendToPlayer(new PacketTankLevelUpdate(getBlockPos(), tankIndex, fluid, capacity), player);
	}
}
