package forestry.mail.network.packets;


import com.mojang.authlib.GameProfile;
import forestry.api.mail.IMailAddress;
import forestry.mail.letters.MailAddress;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.gui.ILetterInfoReceiver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketLetterInfoResponsePlayer(int containerId, IMailAddress address) implements CustomPacketPayload {
    public static final Type<PacketLetterInfoResponsePlayer> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("letter_info_response_player"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoResponsePlayer msg) {
		GameProfile profile = msg.address.getPlayerProfile();
		buffer.writeVarInt(msg.containerId);
		buffer.writeUUID(profile.id());
		buffer.writeUtf(profile.name(), 64);
	}

	public static PacketLetterInfoResponsePlayer decode(RegistryFriendlyByteBuf buffer) {
		return new PacketLetterInfoResponsePlayer(buffer.readVarInt(), new MailAddress(new GameProfile(buffer.readUUID(), buffer.readUtf(64))));
	}

	public static void handle(PacketLetterInfoResponsePlayer msg, Player player) {
		if (player.containerMenu.containerId == msg.containerId && player.containerMenu.stillValid(player) && player.containerMenu instanceof ILetterInfoReceiver receiver) {
			receiver.handleLetterInfoUpdate(PostalCarriers.PLAYER.get(), msg.address, null);
		}
	}
}
