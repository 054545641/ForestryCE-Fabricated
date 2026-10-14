package forestry.mail.network.packets;

import forestry.mail.network.MailPacketIds;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.IMailAddress;
import forestry.mail.letters.MailAddress;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.gui.ILetterInfoReceiver;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PacketLetterInfoResponsePlayer(IMailAddress address) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return MailPacketIds.LETTER_INFO_RESPONSE_PLAYER;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoResponsePlayer msg) {
		GameProfile profile = msg.address.getPlayerProfile();
		buffer.writeUUID(profile.id());
		buffer.writeUtf(profile.name());
	}

	public static PacketLetterInfoResponsePlayer decode(RegistryFriendlyByteBuf buffer) {
		return new PacketLetterInfoResponsePlayer(new MailAddress(new GameProfile(buffer.readUUID(), buffer.readUtf())));
	}

	public static void handle(PacketLetterInfoResponsePlayer msg, Player player) {
		if (player.containerMenu instanceof ILetterInfoReceiver receiver) {
			receiver.handleLetterInfoUpdate(PostalCarriers.PLAYER.value(), msg.address, null);
		}
	}
}
