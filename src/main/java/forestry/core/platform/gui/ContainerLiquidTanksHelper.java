package forestry.core.platform.gui;

import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.capabilities.Capabilities;

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
import net.neoforged.neoforge.fluids.FluidType;

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
			NetworkUtil.sendToServer(new PacketPipetteClick(slot));
		}
	}

	@Override
	public void handlePipetteClick(int slot, ServerPlayer player) {
		ItemStack itemstack = player.containerMenu.getCarried();
		Item held = itemstack.getItem();
		if (!(held instanceof IToolPipette pipette)) {
			return;
		}

		StandardTank tank = this.tile.getTankManager().getTank(slot);
		if (tank == null) return;
		var access = ItemAccess.forPlayerCursor(player, player.containerMenu).oneByOne();
		var container = access.getCapability(Capabilities.Fluid.ITEM);
		if (container == null) return;
		try (var tx = Transaction.openRoot()) {
			int moved = pipette.canPipette(itemstack) && !tank.isEmpty()
				? ResourceHandlerUtil.move(tank.internal(), container, fluid -> true, FluidType.BUCKET_VOLUME, tx)
				: ResourceHandlerUtil.move(container, tank, fluid -> true, FluidType.BUCKET_VOLUME, tx);
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
