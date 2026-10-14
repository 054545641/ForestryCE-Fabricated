package forestry.apiculture.network.packets;

import forestry.apiculture.network.ApiculturePacketIds;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.IBeekeepingLogic;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

// Similar to PacketGuiStream
public record PacketBeeLogicActive(
	BlockPos pos,
	// null on client side
	IBeekeepingLogic logic,
	// null on server side
	RegistryFriendlyByteBuf payload
) implements CustomPacketPayload {
	public PacketBeeLogicActive(IBeeHousing tile) {
		this(tile.getBlockPos(), tile.getBeekeepingLogic(), null);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return ApiculturePacketIds.BEE_LOGIC_ACTIVE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketBeeLogicActive msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writePayloadBuffer(buffer, msg.logic::writeData);
	}

	public static PacketBeeLogicActive decode(RegistryFriendlyByteBuf buffer) {
		return new PacketBeeLogicActive(buffer.readBlockPos(), null, NetworkUtil.readPayloadBuffer(buffer));
	}

	public static void handle(PacketBeeLogicActive msg, Player player) {
		try {
			IBeeHousing beeHousing = TileUtil.getTile(player.level(), msg.pos, IBeeHousing.class);
			if (beeHousing != null) {
				IBeekeepingLogic beekeepingLogic = beeHousing.getBeekeepingLogic();
				beekeepingLogic.readData(msg.payload);
			}
		} finally {
			msg.payload.release();
		}
	}
}
