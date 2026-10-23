package forestry.core.content.sorting.network.packets;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.filter.IFilterRuleType;
import forestry.core.content.sorting.gui.ContainerGeneticFilter;
import forestry.core.platform.client.ClientsideCode;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
public record PacketFilterChangeRule(int containerId, BlockPos pos, Direction facing, String ruleId) implements CustomPacketPayload {
    public static final Type<PacketFilterChangeRule> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("filter_change_rule"));
    public PacketFilterChangeRule(BlockPos pos, Direction facing, IFilterRuleType rule) { this(ClientsideCode.currentContainerId(), pos, facing, rule.getId()); }
    @Override public Type<PacketFilterChangeRule> type() { return TYPE; }
    public static void encode(RegistryFriendlyByteBuf buffer, PacketFilterChangeRule msg) { buffer.writeVarInt(msg.containerId); buffer.writeBlockPos(msg.pos); buffer.writeEnum(msg.facing); buffer.writeUtf(msg.ruleId, 256); }
    public static PacketFilterChangeRule decode(RegistryFriendlyByteBuf buffer) { return new PacketFilterChangeRule(buffer.readVarInt(), buffer.readBlockPos(), buffer.readEnum(Direction.class), buffer.readUtf(256)); }
    public static void handle(PacketFilterChangeRule msg, ServerPlayer player) {
        if (!(player.containerMenu instanceof ContainerGeneticFilter menu) || menu.containerId != msg.containerId || !menu.stillValid(player) || !menu.getTile().getBlockPos().equals(msg.pos)) return;
        var rule = IForestryApi.INSTANCE.getFilterManager().getRule(msg.ruleId);
        if (rule == null) return;
        var logic = menu.getTile().getLogic();
        if (logic.setRule(msg.facing, rule)) { menu.getTile().setChanged(); logic.getNetworkHandler().sendToPlayers(logic, player.level(), player); }
    }
}
