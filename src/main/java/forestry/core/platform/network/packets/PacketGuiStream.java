package forestry.core.platform.network.packets;

import forestry.api.core.ILocationProvider;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

// Streamable is used on the server side to serialize the packet data (payload is null)
// Payload is used on the client side to sync the packet data (streamable is null)
public record PacketGuiStream(
	BlockPos pos,
	// null on client side
	IStreamableGui guiStreamable,
	// null on server side
	RegistryFriendlyByteBuf payload
) implements CustomPacketPayload {
	public <T extends IStreamableGui & ILocationProvider> PacketGuiStream(T guiStreamable) {
		this(guiStreamable.getBlockPos(), guiStreamable, null);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.GUI_UPDATE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiStream msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writePayloadBuffer(buffer, msg.guiStreamable::writeGuiData);
	}

	public static PacketGuiStream decode(RegistryFriendlyByteBuf data) {
		return new PacketGuiStream(data.readBlockPos(), null, NetworkUtil.readPayloadBuffer(data));
	}

	public static void handle(PacketGuiStream msg, Player player) {
		try {
			IStreamableGui tile = TileUtil.getTile(player.level(), msg.pos, IStreamableGui.class);
			if (tile != null) {
				tile.readGuiData(msg.payload);
			}
		} finally {
			msg.payload.release();
		}
	}
}
