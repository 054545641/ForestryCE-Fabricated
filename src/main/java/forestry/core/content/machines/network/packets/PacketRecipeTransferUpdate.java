package forestry.core.content.machines.network.packets;

import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.content.machines.tiles.TileCarpenter;
import forestry.core.content.machines.tiles.TileFabricator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record PacketRecipeTransferUpdate(BlockPos pos,
										 NonNullList<ItemStack> craftingInventory) implements CustomPacketPayload {
    public static final Type<PacketRecipeTransferUpdate> TYPE = new Type<>(forestry.api.ForestryConstants.forestry("recipe_transfer_update"));
    public PacketRecipeTransferUpdate {
        pos = pos.immutable();
        NonNullList<ItemStack> snapshot = NonNullList.withSize(craftingInventory.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < snapshot.size(); slot++) snapshot.set(slot, craftingInventory.get(slot).copy());
        craftingInventory = snapshot;
    }

    @Override
    public NonNullList<ItemStack> craftingInventory() {
        NonNullList<ItemStack> snapshot = NonNullList.withSize(this.craftingInventory.size(), ItemStack.EMPTY);
        for (int slot = 0; slot < snapshot.size(); slot++) snapshot.set(slot, this.craftingInventory.get(slot).copy());
        return snapshot;
    }
	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void encode(RegistryFriendlyByteBuf buffer, PacketRecipeTransferUpdate msg) {
		buffer.writeBlockPos(msg.pos);
		NetworkUtil.writeItemStacks(buffer, msg.craftingInventory);
	}

	public static PacketRecipeTransferUpdate decode(RegistryFriendlyByteBuf buffer) {
		BlockPos pos = buffer.readBlockPos();
		int size = buffer.readVarInt();
		if (size != 9) throw new io.netty.handler.codec.DecoderException("Recipe transfer requires exactly nine ghost slots");
		NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
		for (int index = 0; index < 9; index++) items.set(index, ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer));
		return new PacketRecipeTransferUpdate(pos, items);
	}

	public static void handle(PacketRecipeTransferUpdate msg, Player player) {
		if (msg.craftingInventory.size() != 9 || !player.level().hasChunkAt(msg.pos)) return;
		BlockEntity tile = TileUtil.getTile(player.level(), msg.pos);
		if (tile instanceof TileCarpenter carpenter) {
			int index = 0;
			for (ItemStack stack : msg.craftingInventory) {
				carpenter.getCraftingInventory().setItem(index, stack);
				index++;
			}
		} else if (tile instanceof TileFabricator fabricator) {
			int index = 0;
			for (ItemStack stack : msg.craftingInventory) {
				fabricator.getCraftingInventory().setItem(index, stack);
				index++;
			}
		}
	}
}
