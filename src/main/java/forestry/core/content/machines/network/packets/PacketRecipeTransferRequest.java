package forestry.core.content.machines.network.packets;

import forestry.core.platform.network.PacketIdServer;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.content.machines.gui.ContainerCarpenter;
import forestry.core.content.machines.gui.ContainerFabricator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record PacketRecipeTransferRequest(BlockPos pos,
										  NonNullList<ItemStack> craftingInventory) implements CustomPacketPayload {
	public static void handle(PacketRecipeTransferRequest msg, ServerPlayer player) {
		// Transfers edit a ghost grid only while its actual menu is open and usable.
		if (msg.craftingInventory.size() != 9) return;
		BlockEntity tile;
		net.minecraft.world.Container grid;
		if (player.containerMenu instanceof ContainerCarpenter menu) {
			tile = menu.getCarpenter();
			grid = menu.getCarpenter().getCraftingInventory();
		} else if (player.containerMenu instanceof ContainerFabricator menu) {
			tile = menu.getTile();
			grid = menu.getTile().getCraftingInventory();
		} else {
			return;
		}
		// Resolve through the open menu, so untrusted positions cannot load remote chunks.
		if (!tile.getBlockPos().equals(msg.pos) || tile.getLevel() != player.level()
			|| !player.containerMenu.stillValid(player)) return;
		NonNullList<ItemStack> normalized = NonNullList.withSize(9, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++) {
			ItemStack stack = msg.craftingInventory.get(i);
			normalized.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
			grid.setItem(i, normalized.get(i));
		}
		grid.setChanged();
		tile.setChanged();
		NetworkUtil.sendNetworkPacket(new PacketRecipeTransferUpdate(msg.pos, normalized), msg.pos, player.level());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.RECIPE_TRANSFER_REQUEST;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketRecipeTransferRequest msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writeItemStacks(buffer, msg.craftingInventory);
	}

	public static PacketRecipeTransferRequest decode(RegistryFriendlyByteBuf buffer) {
		BlockPos pos = buffer.readBlockPos();
		int size = buffer.readVarInt();
		if (size != 9) throw new io.netty.handler.codec.DecoderException("Recipe transfer requires exactly nine ghost slots");
		NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
		for (int i = 0; i < 9; i++) items.set(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
		return new PacketRecipeTransferRequest(pos, items);
	}
}
