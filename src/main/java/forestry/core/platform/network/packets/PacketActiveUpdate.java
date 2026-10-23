package forestry.core.platform.network.packets;

import forestry.api.core.multiblock.IMultiblockComponent;
import forestry.core.platform.tile.IActivatable;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public record PacketActiveUpdate(BlockPos pos, boolean active) implements CustomPacketPayload {
    public static final Type<PacketActiveUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("tile_forestry_active"));
	public PacketActiveUpdate(IActivatable tile) {
		this(tile.getBlockPos(), tile.isActive());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketActiveUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		buffer.writeBoolean(msg.active);
	}

	public static PacketActiveUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketActiveUpdate(buffer.readBlockPos(), buffer.readBoolean());
	}

	public static void handle(PacketActiveUpdate msg, Player player) {
		BlockEntity tile = TileUtil.getTile(player.level(), msg.pos);

		if (tile instanceof IActivatable activatable) {
			activatable.setActive(msg.active);
		} else if (tile instanceof IMultiblockComponent component) {
			if (component.getMultiblockLogic().isConnected() && component.getMultiblockLogic().getController() instanceof IActivatable activatable) {
				activatable.setActive(msg.active);
			}
		}
	}
}
