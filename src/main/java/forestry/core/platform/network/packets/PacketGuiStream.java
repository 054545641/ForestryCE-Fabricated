package forestry.core.platform.network.packets;

import forestry.api.core.ILocationProvider;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketGuiStream(int containerId, BlockPos pos, byte[] payload) implements CustomPacketPayload {
    public static final Type<PacketGuiStream> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("gui_update"));
    public PacketGuiStream { pos = pos.immutable(); payload = payload.clone(); NetworkUtil.boundedLength(payload.length, NetworkUtil.MAX_PAYLOAD_BYTES); }
    @Override public byte[] payload() { return payload.clone(); }
    public <T extends IStreamableGui & ILocationProvider> PacketGuiStream(int containerId, T tile) {
        this(containerId, tile.getBlockPos(), NetworkUtil.encodePayload(tile.getLevel().registryAccess(), tile::writeGuiData));
    }
    @Override public Type<PacketGuiStream> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiStream msg) {
        buffer.writeVarInt(msg.containerId); buffer.writeBlockPos(msg.pos); buffer.writeInt(msg.payload.length); buffer.writeBytes(msg.payload);
    }
    public static PacketGuiStream decode(RegistryFriendlyByteBuf buffer) {
        return new PacketGuiStream(buffer.readVarInt(), buffer.readBlockPos(), NetworkUtil.readPayloadBytes(buffer));
    }
    public static void handle(PacketGuiStream msg, Player player) {
        if (player.containerMenu instanceof ContainerTile<?> menu && menu.containerId == msg.containerId
                && menu.stillValid(player) && menu.getTile().getBlockPos().equals(msg.pos) && menu.getTile() instanceof IStreamableGui tile)
            NetworkUtil.applyPayload(player.registryAccess(), msg.payload, tile::readGuiData);
    }
}
