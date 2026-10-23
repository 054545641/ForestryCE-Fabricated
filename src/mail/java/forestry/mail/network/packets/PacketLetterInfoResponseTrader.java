package forestry.mail.network.packets;


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

public record PacketLetterInfoResponseTrader(int containerId, @Nullable ITradeStationInfo info) implements CustomPacketPayload {
    public static final Type<PacketLetterInfoResponseTrader> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("letter_info_response_trader"));
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketLetterInfoResponseTrader msg) {
		buffer.writeVarInt(msg.containerId);
		if (msg.info == null) {
			buffer.writeBoolean(false);
		} else {
			buffer.writeBoolean(true);
			buffer.writeUtf(msg.info.address().getName(), 64);

			GameProfile profile = msg.info.owner();
			buffer.writeBoolean(profile != null);
			if (profile != null) {
				buffer.writeUUID(profile.id());
				buffer.writeUtf(profile.name(), 64);
			}

			ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, msg.info.tradegood());
			NetworkUtil.writeItemStacks(buffer, msg.info.required());

			buffer.writeEnum(msg.info.state());
		}
	}

	public static PacketLetterInfoResponseTrader decode(RegistryFriendlyByteBuf buffer) {
		int containerId = buffer.readVarInt();
		if (buffer.readBoolean()) {
			IMailAddress address = new MailAddress(buffer.readUtf(64));
			GameProfile owner = buffer.readBoolean() ? new GameProfile(buffer.readUUID(), buffer.readUtf(64)) : null;
			ItemStack tradegood = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
			NonNullList<ItemStack> required = NetworkUtil.readItemStacks(buffer);
			EnumTradeStationState state = buffer.readEnum(EnumTradeStationState.class);
			if (required.size() > 4) throw new io.netty.handler.codec.DecoderException("Trade requires more than four payment slots");
			return new PacketLetterInfoResponseTrader(containerId, new TradeStationInfo(address, owner, tradegood, required, state));
		} else {
			return new PacketLetterInfoResponseTrader(containerId, null);
		}
	}

	public static void handle(PacketLetterInfoResponseTrader msg, Player player) {
		if (player.containerMenu.containerId == msg.containerId && player.containerMenu.stillValid(player) && player.containerMenu instanceof ILetterInfoReceiver receiver) {
			receiver.handleLetterInfoUpdate(PostalCarriers.TRADER.get(), null, msg.info);
		}
	}
}
