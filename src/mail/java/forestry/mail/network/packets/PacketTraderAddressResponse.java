package forestry.mail.network.packets;

import forestry.api.mail.IMailAddress;
import forestry.core.platform.tile.TileUtil;
import forestry.mail.letters.MailAddress;
import forestry.mail.tradestation.TradeStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketTraderAddressResponse(BlockPos pos, IMailAddress address) implements CustomPacketPayload {
    public static final Type<PacketTraderAddressResponse> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("trading_address_response"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTraderAddressResponse msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeUtf(msg.address.getName());
	}

	public static PacketTraderAddressResponse decode(RegistryFriendlyByteBuf buffer) {
		return new PacketTraderAddressResponse(buffer.readBlockPos(), new MailAddress(buffer.readUtf()));
	}

	public static void handle(PacketTraderAddressResponse msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, TradeStationBlockEntity.class, tile -> tile.setAddress(msg.address));
	}
}
