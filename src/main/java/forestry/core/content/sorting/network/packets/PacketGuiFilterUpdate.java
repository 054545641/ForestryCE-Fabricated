package forestry.core.content.sorting.network.packets;
import forestry.api.ForestryCapabilities;
import forestry.core.content.sorting.FilterLogic;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
/** Domain references are resolved on the receiving game thread, never Netty. */
public record PacketGuiFilterUpdate(BlockPos pos, byte[] payload) implements CustomPacketPayload {
    public static final Type<PacketGuiFilterUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("gui_update_filter"));
    public PacketGuiFilterUpdate { pos = pos.immutable(); payload = payload.clone(); NetworkUtil.boundedLength(payload.length, 32768); }
    @Override public byte[] payload() { return payload.clone(); }
    @Override public Type<PacketGuiFilterUpdate> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketGuiFilterUpdate msg) { buffer.writeBlockPos(msg.pos); buffer.writeInt(msg.payload.length); buffer.writeBytes(msg.payload); }
    public static PacketGuiFilterUpdate decode(RegistryFriendlyByteBuf buffer) { return new PacketGuiFilterUpdate(buffer.readBlockPos(), NetworkUtil.readPayloadBytes(buffer)); }
    public static void handle(PacketGuiFilterUpdate msg, Player player) {
        TileUtil.getInterface(player.level(), msg.pos, ForestryCapabilities.FILTER_LOGIC, null).ifPresent(logic -> {
            if (logic instanceof FilterLogic filter) NetworkUtil.applyPayload(player.registryAccess(), msg.payload, filter::readGuiData);
        });
    }
}
