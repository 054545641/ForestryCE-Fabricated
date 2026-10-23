package forestry.mail.network.packets;

import forestry.core.platform.tile.TileUtil;
import forestry.mail.tradestation.TradeStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record PacketTraderAddressRequest(int containerId, BlockPos pos, String addressName) implements CustomPacketPayload {
    public static final Type<PacketTraderAddressRequest> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("trading_address_request"));
	public PacketTraderAddressRequest(TradeStationBlockEntity tile, String addressName) {
		this(forestry.core.platform.client.ClientsideCode.currentContainerId(), tile.getBlockPos(), addressName);
	}

	public static void handle(PacketTraderAddressRequest msg, ServerPlayer player) {
        if (!(player.containerMenu instanceof forestry.mail.gui.TradeStationNamingMenu menu)
                || menu.containerId != msg.containerId || !menu.stillValid(player)
                || !menu.getTile().getBlockPos().equals(msg.pos) || msg.addressName.isBlank()) return;
        var tile = menu.getTile();
        var owner = tile.getOwnerHandler().getOwner();
        if (owner != null && !owner.id().equals(player.getUUID())) return;
        if (tile.handleSetAddressRequest(msg.addressName)) player.openMenu(tile);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTraderAddressRequest msg) {
		buffer.writeVarInt(msg.containerId);
		buffer.writeBlockPos(msg.pos);
		buffer.writeUtf(msg.addressName, 64);
	}

	public static PacketTraderAddressRequest decode(RegistryFriendlyByteBuf buffer) {
		return new PacketTraderAddressRequest(buffer.readVarInt(), buffer.readBlockPos(), buffer.readUtf(64));
	}
}
