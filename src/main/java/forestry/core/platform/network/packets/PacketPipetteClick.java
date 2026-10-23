package forestry.core.platform.network.packets;

import forestry.core.platform.gui.IContainerLiquidTanks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record PacketPipetteClick(int containerId, int slot) implements CustomPacketPayload {
    public static final Type<PacketPipetteClick> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("pipette_click"));
	public static void handle(PacketPipetteClick msg, ServerPlayer player) {
		if (player.containerMenu.containerId == msg.containerId && player.containerMenu.stillValid(player) && player.containerMenu instanceof IContainerLiquidTanks tanksMenu) {
			tanksMenu.handlePipetteClick(msg.slot(), player);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketPipetteClick msg) {
		buffer.writeVarInt(msg.containerId);
		buffer.writeVarInt(msg.slot);
	}

	public static PacketPipetteClick decode(RegistryFriendlyByteBuf buffer) {
		return new PacketPipetteClick(buffer.readVarInt(), buffer.readVarInt());
	}
}
