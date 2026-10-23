package forestry.mail.inventory;

import com.google.common.collect.ImmutableSet;
import forestry.api.core.ForestryError;
import forestry.api.core.IError;
import forestry.api.core.IErrorSource;
import forestry.api.mail.ILetter;
import forestry.core.platform.inventory.ItemInventory;
import forestry.core.platform.item.WithScreenItem;
import forestry.core.platform.util.SlotUtil;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterProperties;
import forestry.mail.letters.LetterUtils;
import forestry.mail.letters.MailAddress;
import forestry.mail.letters.PostageUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LetterInventory extends ItemInventory implements IErrorSource {
	private final ILetter letter;

	public LetterInventory(Player player, ItemStack itemstack) {
		super(player, 0, itemstack);
		Letter letter = LetterUtils.getLetterData(itemstack);
        this.letter = letter == null ? new Letter(new MailAddress(player.getGameProfile()), null) : letter;
		((Letter) this.letter).setInventoryChangedListener(() -> LetterUtils.setLetterData(getParent(), this.letter));
	}

	private HolderLookup.Provider getRegistries() {
		return this.player.level().registryAccess();
	}

	@Override
	protected boolean usesComponentInventoryStorage() {
		return false;
	}

	@Override
	protected void writeInventoryToParent(ItemStack parent) {
		// Letter contents are stored in MailDataComponents.LETTER_DATA.get(), not ItemInventory's legacy custom_data storage.
	}

	public ILetter getLetter() {
		return this.letter;
	}

	public void onLetterClosed() {
		ItemStack parent = getParent();
		setParentStack(LetterProperties.closeLetter(parent, this.letter, getRegistries()));
	}

	public void onLetterOpened() {
		ItemStack parent = getParent();
		setParentStack(LetterProperties.openLetter(parent));
	}

	private void setParentStack(ItemStack stack) {
		InteractionHand hand = getHand();
		setParent(stack);
		if (hand != null) {
			this.player.setItemInHand(hand, stack);
		}
	}

	@Override
	public net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> getTransferHandler() { return this.letter.getTransferHandler(); }

	@Override
	public ItemStack removeItem(int index, int count) {
		ItemStack result = this.letter.removeItem(index, count);
		LetterUtils.setLetterData(getParent(), this.letter);
		return result;
	}

	@Override
	public void setItem(int index, ItemStack itemstack) {
        this.letter.setItem(index, itemstack);
		LetterUtils.setLetterData(getParent(), this.letter);
	}

	@Override
	public ItemStack getItem(int i) {
		return this.letter.getItem(i);
	}

	@Override
	public int getContainerSize() {
		return this.letter.getContainerSize();
	}

	@Override
	public int getMaxStackSize() {
		return this.letter.getMaxStackSize();
	}

	@Override
	public boolean stillValid(Player player) {
		return super.stillValid(player) && this.letter.stillValid(player);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack removed = this.letter.removeItemNoUpdate(slot);
		LetterUtils.setLetterData(getParent(), this.letter);
		return removed;
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (this.letter.isProcessed()) {
			return false;
		} else if (SlotUtil.isSlotInRange(slotIndex, Letter.SLOT_POSTAGE_1, Letter.SLOT_POSTAGE_COUNT)) {
			return PostageUtil.isStamp(stack);
		} else if (SlotUtil.isSlotInRange(slotIndex, Letter.SLOT_ATTACHMENT_1, Letter.SLOT_ATTACHMENT_COUNT)) {
			return !(stack.getItem() instanceof WithScreenItem);
		}
		return false;
	}

	/* IErrorSource */
	@Override
	public ImmutableSet<IError> getErrors() {

		ImmutableSet.Builder<IError> errorStates = ImmutableSet.builder();

		if (!this.letter.hasRecipient()) {
			errorStates.add(ForestryError.NO_RECIPIENT);
		}

		if (!this.letter.isProcessed() && !this.letter.isPostPaid()) {
			errorStates.add(ForestryError.NOT_POST_PAID);
		}

		return errorStates.build();
	}
}
