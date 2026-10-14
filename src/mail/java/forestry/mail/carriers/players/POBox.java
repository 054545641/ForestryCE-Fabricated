package forestry.mail.carriers.players;

import com.google.common.base.Preconditions;
import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.api.mail.ILetter;
import forestry.api.mail.IMailAddress;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.util.InventoryUtil;
import forestry.api.mail.IWatchable;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterUtils;
import forestry.mail.letters.MailAddress;
import forestry.mail.carriers.PostalCarriers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class POBox implements forestry.api.core.ITransactionalContainer, IWatchable, INbtReadable, INbtWritable {
	public static final short SLOT_SIZE = 84;

	@Nullable
	private IMailAddress address;
	private final InventoryAdapter letters = new InventoryAdapter(SLOT_SIZE, "Letters").disableAutomation();

	{ this.letters.setInventoryOwner(this::setDirty); }

	private final Set<Watcher> updateWatchers = new HashSet<>();

	public POBox(IMailAddress address) {
		if (!address.getCarrier().equals(PostalCarriers.PLAYER.value())) {
			throw new IllegalArgumentException("POBox address must be a player");
		}

		this.address = address;
	}

	public POBox(CompoundTag tag, HolderLookup.Provider registries) {
		read(tag, registries);
	}

	@Override
	public void read(CompoundTag tag, HolderLookup.Provider registries) {
		if (tag.contains("address")) {
			this.address = new MailAddress(tag.getCompoundOrEmpty("address"));
		}

		this.letters.read(tag, registries);
	}

	@Override
	public CompoundTag write(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		if (this.address != null) {
			CompoundTag nbt = new CompoundTag();
			this.address.write(nbt, registries);
			compoundNBT.put("address", nbt);
		}
		this.letters.write(compoundNBT, registries);
		return compoundNBT;
	}

	public boolean storeLetter(ItemStack stack, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
		ItemStack delivered = stack.copyWithCount(1);
		ILetter letter = LetterUtils.getLetter(delivered);
		Preconditions.checkNotNull(letter, "Letter stack must be a valid letter");
		letter.setProcessed(true);
		letter.invalidatePostage();
		LetterUtils.setLetterData(delivered, letter);
		try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(transaction)) {
			if (!InventoryUtil.tryAddStack(this.letters, delivered, true)) return false;
			tx.commit();
			return true;
		}
	}

	@Override
	public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getTransferHandler() { return this.letters.getTransferHandler(); }

	public POBoxInfo getPOBoxInfo() {
		int playerLetters = 0;
		int tradeLetters = 0;
		for (int i = 0; i < this.letters.getContainerSize(); i++) {
			if (this.letters.getItem(i).isEmpty()) {
				continue;
			}
			Letter letter = LetterUtils.getLetterData(this.letters.getItem(i));
			if (letter != null) {
				if (letter.getSender().getCarrier().equals(PostalCarriers.PLAYER.value())) {
					playerLetters++;
				} else {
					tradeLetters++;
				}
			}
		}

		return new POBoxInfo(playerLetters, tradeLetters);
	}

	@Override
	public boolean isEmpty() {
		return this.letters.isEmpty();
	}

	@Override
	public void setDirty() {
		this.updateWatchers.forEach(Watcher::onWatchableUpdate);
	}

	@Override
	public boolean registerUpdateWatcher(Watcher updateWatcher) {
		return this.updateWatchers.add(updateWatcher);
	}

	@Override
	public boolean unregisterUpdateWatcher(Watcher updateWatcher) {
		return this.updateWatchers.remove(updateWatcher);
	}

	@Override
	public void setItem(int var1, ItemStack var2) {
		this.letters.setItem(var1, var2);
		this.setDirty();
	}

	@Override
	public int getContainerSize() {
		return this.letters.getContainerSize();
	}

	@Override
	public ItemStack getItem(int var1) {
		return this.letters.getItem(var1);
	}

	@Override
	public ItemStack removeItem(int var1, int var2) {
		ItemStack removed = this.letters.removeItem(var1, var2);
		if (!removed.isEmpty()) setDirty();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack removed = this.letters.removeItemNoUpdate(index);
		if (!removed.isEmpty()) setDirty();
		return removed;
	}

	@Override
	public int getMaxStackSize() {
		return this.letters.getMaxStackSize();
	}

	@Override
	public void setChanged() { setDirty(); }

	@Override
	public boolean stillValid(Player var1) {
		return this.letters.stillValid(var1);
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return this.letters.canPlaceItem(i, itemstack);
	}

	@Override
	public void clearContent() { this.letters.clearContent(); setDirty(); }
}
