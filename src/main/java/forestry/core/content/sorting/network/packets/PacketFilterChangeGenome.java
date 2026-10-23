package forestry.core.content.sorting.network.packets;
import forestry.api.core.genetics.ISpecies;
import forestry.core.content.sorting.gui.ContainerGeneticFilter;
import forestry.core.platform.client.ClientsideCode;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;
/** Resolve species on the logical server thread, never on the Netty decoder thread. */
public record PacketFilterChangeGenome(int containerId, BlockPos pos, Direction facing, short index, boolean active, @Nullable Identifier speciesId) implements CustomPacketPayload {
    public static final Type<PacketFilterChangeGenome> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("filter_change_genome"));
    public PacketFilterChangeGenome(BlockPos pos, Direction facing, short index, boolean active, @Nullable ISpecies<?> species) { this(ClientsideCode.currentContainerId(), pos, facing, index, active, species == null ? null : species.id()); }
    @Override public Type<PacketFilterChangeGenome> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketFilterChangeGenome msg) {
        buffer.writeVarInt(msg.containerId); buffer.writeBlockPos(msg.pos); buffer.writeEnum(msg.facing); buffer.writeShort(msg.index); buffer.writeBoolean(msg.active);
        buffer.writeBoolean(msg.speciesId != null); if (msg.speciesId != null) buffer.writeIdentifier(msg.speciesId);
    }
    public static PacketFilterChangeGenome decode(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt(); var pos = buffer.readBlockPos(); var facing = buffer.readEnum(Direction.class); short index = buffer.readShort();
        if (index < 0 || index >= 3) throw new io.netty.handler.codec.DecoderException("Invalid genetic filter index");
        boolean active = buffer.readBoolean(); return new PacketFilterChangeGenome(id, pos, facing, index, active, buffer.readBoolean() ? buffer.readIdentifier() : null);
    }
    public static void handle(PacketFilterChangeGenome msg, ServerPlayer player) {
        if (msg.index < 0 || msg.index >= 3 || !(player.containerMenu instanceof ContainerGeneticFilter menu) || menu.containerId != msg.containerId || !menu.stillValid(player) || !menu.getTile().getBlockPos().equals(msg.pos)) return;
        var species = msg.speciesId == null ? null : SpeciesUtil.getAnySpecies(msg.speciesId);
        if (msg.speciesId != null && species == null) return;
        var logic = menu.getTile().getLogic();
        if (logic.setGenomeFilter(msg.facing, msg.index, msg.active, species)) { menu.getTile().setChanged(); logic.getNetworkHandler().sendToPlayers(logic, player.level(), player); }
    }
}
