package forestry.arboriculture.network;

import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketRipeningUpdate(BlockPos pos, int value) implements CustomPacketPayload {
    public static final Type<PacketRipeningUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("ripening_update"));
	public PacketRipeningUpdate(LeavesBlockEntity leaves) {
		this(leaves.getBlockPos(), leaves.getFruitColour());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketRipeningUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeVarInt(msg.value);
	}

	public static PacketRipeningUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketRipeningUpdate(buffer.readBlockPos(), buffer.readVarInt());
	}

	public static void handle(PacketRipeningUpdate msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, IRipeningPacketReceiver.class, tile -> tile.fromRipeningPacket(msg.value));
	}
}
