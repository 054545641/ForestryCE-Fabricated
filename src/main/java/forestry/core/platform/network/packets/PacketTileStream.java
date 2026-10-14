package forestry.core.platform.network.packets;

import forestry.core.platform.network.IStreamable;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class PacketTileStream implements CustomPacketPayload {
	protected final BlockPos pos;
	@Nullable
	protected final IStreamable streamable;
	@Nullable
	protected final RegistryFriendlyByteBuf payload;

	public <T extends BlockEntity & IStreamable> PacketTileStream(T streamable) {
		this.pos = streamable.getBlockPos();
		this.streamable = streamable;
		this.payload = null;
	}

	private PacketTileStream(BlockPos pos, RegistryFriendlyByteBuf payload) {
		this.pos = pos;
		this.streamable = null;
		this.payload = payload;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.TILE_FORESTRY_UPDATE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTileStream msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writePayloadBuffer(buffer, msg.streamable::writeData);
	}

	public static PacketTileStream decode(RegistryFriendlyByteBuf data) {
		return new PacketTileStream(data.readBlockPos(), NetworkUtil.readPayloadBuffer(data));
	}

	public static void handle(PacketTileStream msg, Player player) {
		try {
			IStreamable tile = TileUtil.getTile(player.level(), msg.pos, IStreamable.class);

			if (tile != null) {
				tile.readData(msg.payload);
			}
		} finally {
			msg.payload.release();
		}
	}
}
