package forestry.apiculture.network.packets;

import forestry.core.platform.network.IStreamable;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Owned bytes can safely be discarded when a connection closes before handling. */
public record PacketBeeLogicActive(BlockPos pos, byte[] payload) implements CustomPacketPayload {
    public static final Type<PacketBeeLogicActive> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("bee_logic_active"));
    public PacketBeeLogicActive { pos = pos.immutable(); payload = payload.clone(); NetworkUtil.boundedLength(payload.length, NetworkUtil.MAX_PAYLOAD_BYTES); }
    @Override public byte[] payload() { return payload.clone(); }
    public PacketBeeLogicActive(forestry.api.apiculture.IBeeHousing tile) {
        this(tile.getBlockPos(), NetworkUtil.encodePayload(tile.getLevel().registryAccess(), tile.getBeekeepingLogic()::writeData));
    }
    @Override public Type<PacketBeeLogicActive> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketBeeLogicActive msg) {
        buffer.writeBlockPos(msg.pos); buffer.writeInt(msg.payload.length); buffer.writeBytes(msg.payload);
    }
    public static PacketBeeLogicActive decode(RegistryFriendlyByteBuf buffer) {
        return new PacketBeeLogicActive(buffer.readBlockPos(), NetworkUtil.readPayloadBytes(buffer));
    }
    public static void handle(PacketBeeLogicActive msg, Player player) {
        var tile = TileUtil.getTile(player.level(), msg.pos, forestry.api.apiculture.IBeeHousing.class);
        if (tile != null) NetworkUtil.applyPayload(player.registryAccess(), msg.payload, tile.getBeekeepingLogic()::readData);
    }
}
