package forestry.core.platform.network.packets;

import forestry.core.platform.fluids.ITankManager;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import forestry.api.core.fluids.FluidVolume;

public record PacketTankLevelUpdate(BlockPos pos, int tankIndex, FluidVolume contents, long capacity) implements CustomPacketPayload {
    public static final Type<PacketTankLevelUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("tank_level_update"));
	public PacketTankLevelUpdate(ILiquidTankTile tileEntity, int tankIndex, FluidVolume contents) {
		this(tileEntity.getBlockPos(), tankIndex, contents, tileEntity.getTankManager().getTank(tankIndex).getCapacity());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTankLevelUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeVarInt(msg.tankIndex);
		FluidVolume.STREAM_CODEC.encode(buffer, msg.contents);
		buffer.writeVarLong(msg.capacity);
	}

	public static PacketTankLevelUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketTankLevelUpdate(buffer.readBlockPos(), buffer.readVarInt(), FluidVolume.STREAM_CODEC.decode(buffer), buffer.readVarLong());
	}

	public static void handle(PacketTankLevelUpdate msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, ILiquidTankTile.class, tile -> {
			ITankManager tankManager = tile.getTankManager();
			tankManager.processTankUpdate(msg.tankIndex, msg.contents, msg.capacity);
		});
	}
}
