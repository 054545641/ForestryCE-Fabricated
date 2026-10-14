package forestry.mail.postoffice;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.ILetter;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostalState;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.tile.TileBase;
import forestry.mail.letters.LetterUtils;
import forestry.mail.letters.MailAddress;
import forestry.mail.carriers.players.POBox;
import forestry.mail.carriers.players.POBoxRegistry;
import forestry.mail.features.MailBlockEntities;
import forestry.mail.gui.MailboxMenu;
import forestry.mail.letters.EnumDeliveryState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class MailboxBlockEntity extends TileBase {
	private final ResourceHandler<ItemResource> automatedMailHandler = new MailDeliveryHandler(stack -> {
		ILetter letter = LetterUtils.getLetter(stack);
		return letter != null && letter.isMailable() && letter.isPostPaid();
	}, (stack, transaction) -> this.level instanceof ServerLevel world
		? PostOffice.getOrCreate(world).lodgeLetter(world, stack, transaction) : EnumDeliveryState.NOT_MAILABLE);

	public MailboxBlockEntity(BlockPos pos, BlockState state) {
		super(MailBlockEntities.MAILBOX.tileType(), pos, state);
		setInternalInventory(new InventoryAdapter(POBox.SLOT_SIZE, "Letters").disableAutomation());
	}

	/* GUI */
	@Override
	public void openGui(ServerPlayer player, InteractionHand hand, BlockPos pos) {
		if (this.level.isClientSide()) {
			return;
		}

		ItemStack heldItem = player.getItemInHand(hand);
		// Handle letter sending
		if (LetterUtils.isLetter(heldItem)) {
			ItemStack singleLetter = heldItem.copyWithCount(1);
			IPostalState result;
			try (var tx = Transaction.openRoot()) {
				// Extract the actual hand stack in the same transaction as the recipient and postage.
				var access = ItemAccess.forPlayerSlot(player, hand == InteractionHand.MAIN_HAND
					? player.getInventory().getSelectedSlot() : net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
				if (access.extract(ItemResource.of(heldItem), 1, tx) != 1) return;
				result = PostOffice.getOrCreate((ServerLevel) this.level).lodgeLetter((ServerLevel) this.level, singleLetter, tx);
				if (result.isOk()) tx.commit();
			}
			if (!result.isOk()) player.sendSystemMessage(result.getDescription());
		} else {
			super.openGui(player, hand, pos);
		}
	}

	/* MAIL HANDLING */
	public ResourceHandler<ItemResource> getAutomatedMailHandler() {
		return this.automatedMailHandler;
	}

	public Container getOrCreateMailInventory(Level world, GameProfile playerProfile) {
		if (world.isClientSide()) {
			return getInternalInventory();
		}

		IMailAddress address = new MailAddress(playerProfile);
		return POBoxRegistry.getOrCreate((ServerLevel) world).getOrCreatePOBox(address);
	}


	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new MailboxMenu(windowId, inv, this);
	}
}
