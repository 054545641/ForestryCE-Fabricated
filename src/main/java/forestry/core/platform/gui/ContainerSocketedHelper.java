package forestry.core.platform.gui;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.engine.circuits.ISolderingIron;
import forestry.core.platform.network.packets.PacketChipsetClick;
import forestry.core.platform.network.packets.PacketSocketUpdate;
import forestry.core.platform.network.packets.PacketSolderingIronClick;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContainerSocketedHelper<T extends BlockEntity & ISocketable> implements IContainerSocketed {

	private final T tile;

	public ContainerSocketedHelper(T tile) {
		this.tile = tile;
	}

	@Override
	public void handleChipsetClick(int slot) {
		NetworkUtil.sendToServer(new PacketChipsetClick(slot));
	}

	@Override
	public void handleChipsetClickServer(int slot, ServerPlayer player, ItemStack itemstack) {
		if (!this.tile.getSocket(slot).isEmpty()) {
			return;
		}

		if (!IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(itemstack)) {
			return;
		}

		ICircuitBoard circuitBoard = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(itemstack);
		if (circuitBoard == null) {
			return;
		}

		if (!this.tile.getSocketType().equals(circuitBoard.getSocketType())) {
			return;
		}

		ItemStack toSocket = itemstack.copy();
		toSocket.setCount(1);
        this.tile.setSocket(slot, toSocket);

		ItemStack stack = player.containerMenu.getCarried();
		stack.shrink(1);
		player.containerMenu.broadcastChanges();

		PacketSocketUpdate packet = PacketSocketUpdate.create(this.tile);
		NetworkUtil.sendToPlayer(packet, player);
	}

	@Override
	public void handleSolderingIronClick(int slot) {
		NetworkUtil.sendToServer(new PacketSolderingIronClick(slot));
	}

	@Override
	public void handleSolderingIronClickServer(int slot, ServerPlayer player, ItemStack itemstack) {
		ItemStack socket = this.tile.getSocket(slot);
		if (socket.isEmpty() || !(itemstack.getItem() instanceof ISolderingIron)) {
			return;
		}

		// Not sufficient space in player's inventory. failed to stow.
		if (!InventoryUtil.stowInInventory(socket, player.getInventory(), false)) {
			return;
		}

        this.tile.setSocket(slot, ItemStack.EMPTY);
		InventoryUtil.stowInInventory(socket, player.getInventory(), true);
		// The soldering iron is on the menu cursor, not equipped in either hand.
		itemstack.hurtAndBreak(1, player.level(), player, brokenItem -> {});
		player.containerMenu.broadcastChanges();

		PacketSocketUpdate packet = PacketSocketUpdate.create(this.tile);
		NetworkUtil.sendToPlayer(packet, player);
	}
}
