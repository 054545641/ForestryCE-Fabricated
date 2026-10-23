package forestry.core.platform.item;

import forestry.core.platform.gui.ContainerItemInventory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public abstract class WithScreenItem extends ItemForestry {
	public WithScreenItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
		ItemStack stack = player.getItemInHand(handIn);

		if (player instanceof ServerPlayer serverPlayer) {
			openGui(serverPlayer, stack, handIn);
		}

		return InteractionResult.SUCCESS;
	}

	protected void openGui(ServerPlayer serverPlayer, ItemStack heldItem, InteractionHand hand) {
        var provider = getMenuProvider(heldItem);
        serverPlayer.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<forestry.core.platform.network.MenuOpeningData>() {
            @Override public net.minecraft.network.chat.Component getDisplayName() { return provider.getDisplayName(); }
            @Override public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) { return provider.createMenu(id, inventory, player); }
            @Override public forestry.core.platform.network.MenuOpeningData getScreenOpeningData(ServerPlayer player) {
                var buffer = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), player.registryAccess());
                try {
                    writeContainerData(player, heldItem, hand, buffer);
                    byte[] bytes = new byte[buffer.readableBytes()]; buffer.readBytes(bytes);
                    return new forestry.core.platform.network.MenuOpeningData(bytes);
                } finally { buffer.release(); }
            }
        });
	}

	public SimpleMenuProvider getMenuProvider(ItemStack heldItem) {
		return new SimpleMenuProvider((windowId, playerInv, player) -> getContainer(windowId, player, heldItem), heldItem.getHoverName());
	}

	protected void writeContainerData(ServerPlayer player, ItemStack stack, InteractionHand hand, RegistryFriendlyByteBuf buffer) {
		// Opening a screen does not start an active item-use action (food, bow, etc.).
		buffer.writeBoolean(hand == InteractionHand.MAIN_HAND);
	}

    // Native menu validity closes this screen when the parent item leaves the player's hands.

	@Nullable
	public abstract AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem);
}
