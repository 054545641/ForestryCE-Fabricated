package forestry.core.platform.network.packets;

import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record PacketSocketUpdate(BlockPos pos, NonNullList<ItemStack> itemStacks) implements CustomPacketPayload {
    public static final Type<PacketSocketUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("socket_update"));
	public static <T extends BlockEntity & ISocketable> PacketSocketUpdate create(T tile) {
		BlockPos pos = tile.getBlockPos();

		NonNullList<ItemStack> itemStacks = NonNullList.withSize(tile.getSocketCount(), ItemStack.EMPTY);
		for (int i = 0; i < tile.getSocketCount(); i++) {
			itemStacks.set(i, tile.getSocket(i));
		}

		return new PacketSocketUpdate(pos, itemStacks);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketSocketUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writeItemStacks(buffer, msg.itemStacks);
	}

	public static PacketSocketUpdate decode(RegistryFriendlyByteBuf buffer) {
		return new PacketSocketUpdate(buffer.readBlockPos(), NetworkUtil.readItemStacks(buffer));
	}

	public static void handle(PacketSocketUpdate msg, Player player) {
		TileUtil.actOnTile(player.level(), msg.pos, ISocketable.class, socketable -> {
            if (msg.itemStacks.size() != socketable.getSocketCount()) return;
			for (int i = 0; i < msg.itemStacks.size(); i++) {
				socketable.setSocket(i, msg.itemStacks.get(i));
			}
		});
	}
}
