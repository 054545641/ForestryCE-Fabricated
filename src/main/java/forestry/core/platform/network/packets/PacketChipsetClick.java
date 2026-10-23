package forestry.core.platform.network.packets;

import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.platform.gui.IContainerSocketed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PacketChipsetClick(int containerId, int slot) implements CustomPacketPayload {
    public static final Type<PacketChipsetClick> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("chipset_click"));
	public static void handle(PacketChipsetClick msg, ServerPlayer player) {
		if (player.containerMenu.containerId == msg.containerId && player.containerMenu.stillValid(player) && player.containerMenu instanceof IContainerSocketed socketMenu) {
			ItemStack itemstack = player.containerMenu.getCarried();
			// todo replace check with tag
			if (itemstack.getItem() instanceof ItemCircuitBoard) {
				socketMenu.handleChipsetClickServer(msg.slot(), player, itemstack);
			}
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketChipsetClick msg) {
		buffer.writeVarInt(msg.containerId);
		buffer.writeVarInt(msg.slot);
	}

	public static PacketChipsetClick decode(RegistryFriendlyByteBuf buffer) {
		return new PacketChipsetClick(buffer.readVarInt(), buffer.readVarInt());
	}
}
