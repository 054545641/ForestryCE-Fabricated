package forestry.mail.network.packets;


import forestry.mail.gui.LetterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record PacketLetterTextSet(int containerId, String string) implements CustomPacketPayload {
    public static final Type<PacketLetterTextSet> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("letter_text_set"));
	public static void handle(PacketLetterTextSet msg, ServerPlayer player) {
		if (player.containerMenu instanceof LetterMenu letterMenu && letterMenu.containerId == msg.containerId && letterMenu.stillValid(player)) {
			letterMenu.handleSetText(msg.string());
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterTextSet msg) {
		buffer.writeVarInt(msg.containerId);
        buffer.writeUtf(msg.string, 128);
	}

	public static PacketLetterTextSet decode(RegistryFriendlyByteBuf buffer) {
		return new PacketLetterTextSet(buffer.readVarInt(), buffer.readUtf(128));
	}
}
