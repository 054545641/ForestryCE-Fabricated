package forestry.core.platform.network.packets;

import forestry.api.core.IErrorLogicSource;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public record PacketErrorUpdate(BlockPos pos, short[] errorStates) implements CustomPacketPayload {
    public static final Type<PacketErrorUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("error_update"));
	public PacketErrorUpdate(BlockEntity tile, IErrorLogicSource errorLogicSource) {
		this(tile.getBlockPos(), errorLogicSource.getErrorLogic().toArray());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketErrorUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writeShortArray(buffer, msg.errorStates);
	}

	public static PacketErrorUpdate decode(RegistryFriendlyByteBuf buffer) {
		BlockPos pos = buffer.readBlockPos();
		short[] errorStats = NetworkUtil.readShortArray(buffer);
		return new PacketErrorUpdate(pos, errorStats);
	}

	public static void handle(PacketErrorUpdate msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, IErrorLogicSource.class, errorSourceTile -> errorSourceTile.getErrorLogic().fromArray(msg.errorStates));
	}
}
