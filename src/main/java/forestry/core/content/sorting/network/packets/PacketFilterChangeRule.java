package forestry.core.content.sorting.network.packets;

import forestry.api.ForestryCapabilities;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.filter.IFilterRuleType;
import forestry.core.platform.network.PacketIdServer;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public record PacketFilterChangeRule(BlockPos pos, Direction facing,
									 IFilterRuleType rule) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdServer.FILTER_CHANGE_RULE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketFilterChangeRule msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeShort(msg.facing.get3DDataValue());
		buffer.writeShort(IForestryApi.INSTANCE.getFilterManager().getId(msg.rule));
	}

	public static PacketFilterChangeRule decode(RegistryFriendlyByteBuf buffer) {
		return new PacketFilterChangeRule(buffer.readBlockPos(), Direction.values()[buffer.readShort()], Objects.requireNonNull(IForestryApi.INSTANCE.getFilterManager().getRule(buffer.readShort())));
	}

	public static void handle(PacketFilterChangeRule msg, ServerPlayer player) {
		TileUtil.getInterface(player.level(), msg.pos(), ForestryCapabilities.FILTER_LOGIC, null).ifPresent(logic -> {
			if (logic.setRule(msg.facing(), msg.rule())) {
				logic.getNetworkHandler().sendToPlayers(logic, player.level(), player);
			}
		});
	}
}
