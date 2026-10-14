package forestry.core.platform.network.packets;

import forestry.core.platform.fluids.ITankManager;
import forestry.core.platform.network.PacketIdClient;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidStack;

public record PacketTankLevelUpdate(BlockPos pos, int tankIndex, FluidStack contents, int capacity) implements CustomPacketPayload {
	public PacketTankLevelUpdate(ILiquidTankTile tileEntity, int tankIndex, FluidStack contents) {
		this(tileEntity.getBlockPos(), tankIndex, contents, tileEntity.getTankManager().getTank(tankIndex).getCapacity());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return PacketIdClient.TANK_LEVEL_UPDATE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketTankLevelUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeVarInt(msg.tankIndex);
		FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer, msg.contents);
		buffer.writeVarInt(msg.capacity);
	}

	public static PacketTankLevelUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketTankLevelUpdate(buffer.readBlockPos(), buffer.readVarInt(), FluidStack.OPTIONAL_STREAM_CODEC.decode(buffer), buffer.readVarInt());
	}

	public static void handle(PacketTankLevelUpdate msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, ILiquidTankTile.class, tile -> {
			ITankManager tankManager = tile.getTankManager();
			tankManager.processTankUpdate(msg.tankIndex, msg.contents, msg.capacity);
		});
	}
}
