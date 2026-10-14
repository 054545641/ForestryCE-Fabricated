package forestry.mail.postoffice;

import forestry.Forestry;
import forestry.api.mail.*;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.EnumDeliveryState;
import forestry.mail.letters.PostageUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import forestry.core.platform.util.SavedDataMigration;
import forestry.mail.letters.LetterUtils;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PostOffice extends SavedData implements IPostOffice {
	public static final String SAVE_NAME = "forestry_mail";
	private static final String KEY_COLLECTED = "collected";
	private static final String KEY_LEGACY = "CPS";

	// Cheapest first, matching the ordinal walk this replaced. The id breaks ties so two mods'
	// stamps of equal value keep a stable order
	private static final Comparator<Item> CHEAPEST_FIRST = (left, right) -> {
		int byPostage = Integer.compare(PostageUtil.getPostage(left), PostageUtil.getPostage(right));
		return byPostage != 0 ? byPostage : BuiltInRegistries.ITEM.getKey(left).compareTo(BuiltInRegistries.ITEM.getKey(right));
	};

	private final Object2IntOpenHashMap<Item> collectedStamps = new Object2IntOpenHashMap<>();
	private final PostageLedger postageJournal = new PostageLedger(this.collectedStamps, this::setDirty);
	private int deliveryDepth;

	public PostOffice() {
	}

	public PostOffice(CompoundTag tag) {
		if (tag.contains(KEY_COLLECTED)) {
			readCollected(tag.getCompound(KEY_COLLECTED).orElseThrow(() -> new IllegalArgumentException("Invalid collected stamps")));
		} else {
			readLegacy(tag);
		}
	}

	private void readCollected(CompoundTag collected) {
		this.postageJournal.readCollected(collected, (key, count) -> Forestry.LOGGER.warn("Post office dropped {} collected stamp(s) of unknown item {}", count, key));
	}

	// Saves from before the postage data map keyed an array by EnumPostage.ordinal(), so CPS1 through
	// CPS7 were P_1 through P_100. CPS0 was never incremented and CPS8 (P_200) never had a stamp item
	private void readLegacy(CompoundTag tag) {
		EnumStampDefinition[] byLegacyIndex = EnumStampDefinition.VALUES;

		for (int i = 1; i <= byLegacyIndex.length; i++) {
			int count = tag.getIntOr(KEY_LEGACY + i, 0);
			if (count > 0) {
				this.collectedStamps.put(MailItems.STAMPS.item(byLegacyIndex[i - 1]), count);
			}
		}

		int dropped = tag.getIntOr(KEY_LEGACY + "0", 0) + tag.getIntOr(KEY_LEGACY + (byLegacyIndex.length + 1), 0);
		if (dropped > 0) {
			Forestry.LOGGER.warn("Post office dropped {} collected stamp(s) from a denomination that never had an item", dropped);
		}

		// A vault nobody deposits into or withdraws from never dirties itself otherwise, so the
		// migrated data could sit unsaved in the new format for the life of the world
		setDirty();
	}

	public CompoundTag save(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		compoundNBT.put(KEY_COLLECTED, this.postageJournal.writeCollected());
		return compoundNBT;
	}

	// / STAMP MANAGMENT
	@Override
	public ItemStack getAnyStamp(int max) {
		List<Item> order = new ArrayList<>(this.collectedStamps.keySet());
		order.sort(CHEAPEST_FIRST);

		for (Item stamp : order) {
			ItemStack withdrawn = getAnyStamp(stamp, max);
			if (!withdrawn.isEmpty()) {
				return withdrawn;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getAnyStamp(Item stamp, int max) {
		int collected = Math.min(Math.max(0, max), this.collectedStamps.getInt(stamp));
		if (collected == 0) return ItemStack.EMPTY;
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			this.postageJournal.updateSnapshots(tx);
			int remaining = this.collectedStamps.getInt(stamp) - collected;
			if (remaining == 0) this.collectedStamps.removeInt(stamp);
			else this.collectedStamps.put(stamp, remaining);
			tx.commit();
			return new ItemStack(stamp, collected);
		}
	}

	@Override
	public void collectPostage(NonNullList<ItemStack> stamps) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			for (ItemStack stamp : stamps) {
				if (stamp.isEmpty() || !PostageUtil.isStamp(stamp)) continue;
				int updated = Math.addExact(this.collectedStamps.getInt(stamp.getItem()), stamp.getCount());
				this.postageJournal.updateSnapshots(tx);
				this.collectedStamps.put(stamp.getItem(), updated);
			}
			tx.commit();
		}
	}

	// / DELIVERY
	@Override
	public IPostalState lodgeLetter(ServerLevel world, ItemStack itemstack, TransactionContext transaction) {
		// Returned/forwarded trade mail must not recurse indefinitely through trader addresses.
		if (this.deliveryDepth >= 32) return EnumDeliveryState.NOT_MAILABLE;
		this.deliveryDepth++;
		try {
			return PostalTransactions.attempt(transaction, true, tx -> {
				ItemStack single = itemstack.copyWithCount(1);
				ILetter letter = LetterUtils.getLetter(single);
				if (letter == null) return EnumDeliveryState.NOT_MAILABLE;
				if (letter.isProcessed()) return EnumDeliveryState.ALREADY_MAILED;
				if (!letter.isPostPaid()) return EnumDeliveryState.NOT_POSTPAID;
				if (!letter.isMailable() || letter.getRecipient() == null || !letter.getRecipient().isValid()) return EnumDeliveryState.NOT_MAILABLE;
				IMailAddress address = letter.getRecipient();
				IPostalState state = address.getCarrier().deliverLetter(world, this, address, single, tx);
				if (!state.isOk()) return state;
				collectPostage(letter.getPostage());
				return EnumDeliveryState.OK;
			});
		} finally {
			this.deliveryDepth--;
		}
	}

	public static PostOffice getOrCreate(ServerLevel level) {
		SavedDataType<PostOffice> type = new SavedDataType<>(Identifier.parse("forestry:mail/post_office"), PostOffice::new,
			CompoundTag.CODEC.xmap(PostOffice::new, data -> data.save(new CompoundTag(), level.registryAccess())));
		return SavedDataMigration.getOrCreate(level, type, SAVE_NAME, PostOffice::new);
	}
}
