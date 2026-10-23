package forestry.core.platform.network.packets;

import forestry.core.platform.network.IStreamable;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Owned bytes can safely be discarded when a connection closes before handling. */
public record PacketTileStream(BlockPos pos, Identifier blockEntityType, byte[] payload) implements CustomPacketPayload {
    public static final Type<PacketTileStream> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("tile_forestry_update"));
    public PacketTileStream { pos = pos.immutable(); java.util.Objects.requireNonNull(blockEntityType); payload = payload.clone(); NetworkUtil.boundedLength(payload.length, NetworkUtil.MAX_PAYLOAD_BYTES); }
    @Override public byte[] payload() { return payload.clone(); }
    public <T extends BlockEntity & IStreamable> PacketTileStream(T tile) {
        this(tile.getBlockPos(), BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(tile.getType()), NetworkUtil.encodePayload(tile.getLevel().registryAccess(), tile::writeData));
    }
    @Override public Type<PacketTileStream> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketTileStream msg) {
        buffer.writeBlockPos(msg.pos); buffer.writeIdentifier(msg.blockEntityType); buffer.writeInt(msg.payload.length); buffer.writeBytes(msg.payload);
    }
    public static PacketTileStream decode(RegistryFriendlyByteBuf buffer) {
        return new PacketTileStream(buffer.readBlockPos(), buffer.readIdentifier(), NetworkUtil.readPayloadBytes(buffer));
    }
    public static void handle(PacketTileStream msg, Player player) {
        BlockEntity entity = TileUtil.getTile(player.level(), msg.pos);
        if (entity instanceof IStreamable tile && msg.blockEntityType.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()))) {
            NetworkUtil.applyPayload(player.registryAccess(), msg.payload, tile::readData);
        }
    }
}
