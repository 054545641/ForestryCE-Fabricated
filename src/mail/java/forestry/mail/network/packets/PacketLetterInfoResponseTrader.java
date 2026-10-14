package forestry.mail.network.packets;

import forestry.mail.network.MailPacketIds;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.EnumTradeStationState;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.ITradeStationInfo;
import forestry.core.platform.util.NetworkUtil;
import forestry.mail.letters.MailAddress;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.carriers.trading.TradeStationInfo;
import forestry.mail.gui.ILetterInfoReceiver;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public record PacketLetterInfoResponseTrader(@Nullable ITradeStationInfo info) implements CustomPacketPayload {
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return MailPacketIds.LETTER_INFO_RESPONSE_TRADER;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoResponseTrader msg) {
		if (msg.info == null) {
			buffer.writeBoolean(false);
		} else {
			buffer.writeBoolean(true);
			buffer.writeUtf(msg.info.address().getName());

			GameProfile profile = msg.info.owner();
			buffer.writeUUID(profile.id());
			buffer.writeUtf(profile.name());

			ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, msg.info.tradegood());
			NetworkUtil.writeItemStacks(buffer, msg.info.required());

			buffer.writeEnum(msg.info.state());
		}
	}

	public static PacketLetterInfoResponseTrader decode(RegistryFriendlyByteBuf buffer) {
		if (buffer.readBoolean()) {
			IMailAddress address = new MailAddress(buffer.readUtf());
			GameProfile owner = new GameProfile(buffer.readUUID(), buffer.readUtf());
			ItemStack tradegood = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
			NonNullList<ItemStack> required = NetworkUtil.readItemStacks(buffer);
			EnumTradeStationState state = buffer.readEnum(EnumTradeStationState.class);
			return new PacketLetterInfoResponseTrader(new TradeStationInfo(address, owner, tradegood, required, state));
		} else {
			return new PacketLetterInfoResponseTrader(null);
		}
	}

	public static void handle(PacketLetterInfoResponseTrader msg, Player player) {
		if (player.containerMenu instanceof ILetterInfoReceiver receiver) {
			receiver.handleLetterInfoUpdate(PostalCarriers.TRADER.value(), null, msg.info);
		}
	}
}
