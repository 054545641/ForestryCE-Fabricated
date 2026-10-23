package forestry.core.platform.gui;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;

import forestry.api.core.IToolPipette;
import forestry.core.platform.fluids.StandardTank;
import forestry.core.platform.network.packets.PacketPipetteClick;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;

import javax.annotation.Nullable;

public class ContainerLiquidTanksHelper<T extends BlockEntity & ILiquidTankTile> implements IContainerLiquidTanks {
	private final T tile;

	public ContainerLiquidTanksHelper(T tile) {
		this.tile = tile;
	}

	@Override
	public void handlePipetteClickClient(int slot, Player player) {
		ItemStack itemstack = player.containerMenu.getCarried();
		if (itemstack.getItem() instanceof IToolPipette) {
			NetworkUtil.sendToServer(new PacketPipetteClick(player.containerMenu.containerId, slot));
		}
	}

	@Override
	public void handlePipetteClick(int slot, ServerPlayer player) {
		ItemStack itemstack = player.containerMenu.getCarried();
		Item held = itemstack.getItem();
		if (!(held instanceof IToolPipette pipette)) {
			return;
		}

		if (!player.containerMenu.stillValid(player) || !forestry.core.platform.tile.TileUtil.isUsableByPlayer(player, this.tile)) return;
		StandardTank tank = this.tile.getTankManager().getTank(slot);
		if (tank == null) return;
		var container = ContainerItemContext.ofPlayerCursor(player, player.containerMenu).find(FluidStorage.ITEM);
		if (container == null) return;
		try (var tx = Transaction.openOuter()) {
			long moved = pipette.canPipette(itemstack) && !tank.isEmpty()
				? StorageUtil.move(tank.internal(), container, fluid -> true, FluidConstants.BUCKET, tx)
				: StorageUtil.move(container, tank, fluid -> true, FluidConstants.BUCKET, tx);
			if (moved > 0) tx.commit();
		}
		player.containerMenu.broadcastChanges();
	}

	@Nullable
	@Override
	public StandardTank getTank(int slot) {
		return this.tile.getTankManager().getTank(slot);
	}
}
