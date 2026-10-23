package forestry.mail.network.packets;


import forestry.api.ForestryRegistries;
import forestry.api.mail.IPostalCarrier;
import forestry.mail.gui.LetterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record PacketLetterInfoRequest(int containerId, String recipientName,
									  IPostalCarrier addressType) implements CustomPacketPayload {
    public static final Type<PacketLetterInfoRequest> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("letter_info_request"));
	public static void handle(PacketLetterInfoRequest msg, ServerPlayer player) {
		if (player.containerMenu instanceof LetterMenu menu && menu.containerId == msg.containerId && menu.stillValid(player)) {
			menu.handleRequestLetterInfo(player, msg.recipientName(), msg.addressType());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoRequest msg) {
		buffer.writeVarInt(msg.containerId);
        buffer.writeUtf(msg.recipientName, 64);
		buffer.writeUtf(ForestryRegistries.POSTAL_CARRIER.getKey(msg.addressType).toString());
	}

	public static PacketLetterInfoRequest decode(RegistryFriendlyByteBuf buffer) {
		int containerId = buffer.readVarInt();
        String recipient = buffer.readUtf(64);
		Identifier id = buffer.readIdentifier();
		IPostalCarrier carrier = ForestryRegistries.POSTAL_CARRIER.getValue(id);
		if (carrier == null) {
			throw new io.netty.handler.codec.DecoderException("Unknown postal carrier " + id);
		}
		return new PacketLetterInfoRequest(containerId, recipient, carrier);
	}
}
