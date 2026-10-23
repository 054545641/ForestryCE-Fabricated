package forestry.core.platform.network.packets;

import forestry.core.platform.gui.IContainerSocketed;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record PacketSolderingIronClick(int containerId, int slot) implements CustomPacketPayload {
    public static final Type<PacketSolderingIronClick> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("soldering_iron_click"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketSolderingIronClick msg) {
		buffer.writeVarInt(msg.containerId);
		buffer.writeVarInt(msg.slot);
	}

	public static PacketSolderingIronClick decode(RegistryFriendlyByteBuf buffer) {
		return new PacketSolderingIronClick(buffer.readVarInt(), buffer.readVarInt());
	}

	public static void handle(PacketSolderingIronClick msg, ServerPlayer player) {
		if (player.containerMenu.containerId == msg.containerId && player.containerMenu.stillValid(player) && player.containerMenu instanceof IContainerSocketed socketMenu) {
			ItemStack itemstack = player.containerMenu.getCarried();

			socketMenu.handleSolderingIronClickServer(msg.slot(), player, itemstack);
		}
	}
}
