package forestry.mail.network.packets;


import forestry.core.platform.config.ForestryConfig;
import forestry.mail.carriers.players.POBoxInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketPOBoxInfoResponse(int playerLetters, int tradeLetters,
									  boolean silent) implements CustomPacketPayload {
    public static final Type<PacketPOBoxInfoResponse> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("pobox_info_response"));
	public PacketPOBoxInfoResponse(POBoxInfo info, boolean silent) {
		this(info.playerLetters(), info.tradeLetters(), silent);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketPOBoxInfoResponse msg) {
		buffer.writeInt(msg.playerLetters);
		buffer.writeInt(msg.tradeLetters);
		buffer.writeBoolean(msg.silent);
	}

	public static PacketPOBoxInfoResponse decode(RegistryFriendlyByteBuf buffer) {
		return new PacketPOBoxInfoResponse(buffer.readInt(), buffer.readInt(), buffer.readBoolean());
	}

    private static java.util.function.BiConsumer<POBoxInfo, Boolean> clientReceiver = (info, silent) -> {};
    public static void installClientReceiver(java.util.function.BiConsumer<POBoxInfo, Boolean> receiver) { clientReceiver = java.util.Objects.requireNonNull(receiver); }
    public static void handle(PacketPOBoxInfoResponse msg, Player player) {
        clientReceiver.accept(new POBoxInfo(msg.playerLetters, msg.tradeLetters), msg.silent);
    }
}
