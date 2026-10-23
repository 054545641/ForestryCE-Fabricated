package forestry.mail.network;

import forestry.core.platform.network.PacketIdServer;
import forestry.mail.network.packets.PacketLetterInfoRequest;
import forestry.mail.network.packets.PacketLetterInfoResponsePlayer;
import forestry.mail.network.packets.PacketLetterInfoResponseTrader;
import forestry.mail.network.packets.PacketLetterTextSet;
import forestry.mail.network.packets.PacketPOBoxInfoResponse;
import forestry.mail.network.packets.PacketTraderAddressRequest;
import forestry.mail.network.packets.PacketTraderAddressResponse;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet ids owned by the mail module, both directions. The path strings are the packets' wire
 * identity and must never change.
 */
public class MailPacketIds {
	// clientbound
	public static final CustomPacketPayload.Type<PacketTraderAddressResponse> TRADING_ADDRESS_RESPONSE = PacketIdServer.type("trading_address_response");
	public static final CustomPacketPayload.Type<PacketLetterInfoResponsePlayer> LETTER_INFO_RESPONSE_PLAYER = PacketIdServer.type("letter_info_response_player");
	public static final CustomPacketPayload.Type<PacketLetterInfoResponseTrader> LETTER_INFO_RESPONSE_TRADER = PacketIdServer.type("letter_info_response_trader");
	public static final CustomPacketPayload.Type<PacketPOBoxInfoResponse> POBOX_INFO_RESPONSE = PacketIdServer.type("pobox_info_response");

	// serverbound
	public static final CustomPacketPayload.Type<PacketLetterInfoRequest> LETTER_INFO_REQUEST = PacketIdServer.type("letter_info_request");
	public static final CustomPacketPayload.Type<PacketTraderAddressRequest> TRADING_ADDRESS_REQUEST = PacketIdServer.type("trading_address_request");
	public static final CustomPacketPayload.Type<PacketLetterTextSet> LETTER_TEXT_SET = PacketIdServer.type("letter_text_set");
}
