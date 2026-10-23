package forestry.mail.letters;

import forestry.api.mail.ILetter;
import forestry.core.features.CoreDataComponents;
import net.minecraft.core.HolderLookup;
import forestry.mail.MailFabricModule;
import forestry.mail.letters.LetterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterUtils;

public class LetterProperties {
	public static ItemStack createStampedLetterStack(ILetter letter) {
		LetterItem.Size size = getSize(letter);
		return MailFabricModule.LETTERS.stack(size, LetterItem.State.STAMPED, 1);
	}

	public static ItemStack closeLetter(ItemStack parent, ILetter letter, HolderLookup.Provider registries) {
		Item item = parent.getItem();
		if (!(item instanceof LetterItem itemLetter)) {
			return parent;
		}
		LetterItem.State state = itemLetter.getState();
		LetterItem.Size size = itemLetter.getSize();

		switch (state) {
			case OPENED:
				if (letter.countAttachments() <= 0) {
					state = LetterItem.State.EMPTIED;
				}
				break;
			case FRESH:
			case STAMPED:
				if (letter.isMailable() && letter.isPostPaid()) {
					state = LetterItem.State.STAMPED;
				} else {
					state = LetterItem.State.FRESH;
				}
				size = getSize(letter);
				break;
			case EMPTIED:
		}
		ItemStack ret = MailFabricModule.LETTERS.stack(size, state, parent.getCount());
		ret.copyFrom(CoreDataComponents.ITEM_INVENTORY_UID.get(), parent);
		LetterUtils.setLetterData(ret, letter);
		return ret;
	}

	public static ItemStack openLetter(ItemStack parent) {
		Item item = parent.getItem();
		if (!(item instanceof LetterItem itemLetter)) {
			return parent;
		}

		LetterItem.State state = itemLetter.getState();
		if (state == LetterItem.State.FRESH || state == LetterItem.State.STAMPED) {
			LetterItem.Size size = itemLetter.getSize();
			ItemStack ret = MailFabricModule.LETTERS.stack(size, state, parent.getCount());
			ret.copyFrom(CoreDataComponents.ITEM_INVENTORY_UID.get(), parent);
			Letter letter = LetterUtils.getLetterData(parent);
			if (letter != null) {
				LetterUtils.setLetterData(ret, letter);
			}
			return ret;
		} else {
			return parent;
		}
	}

	private static LetterItem.Size getSize(ILetter letter) {
		int count = letter.countAttachments();

		if (count > 5) {
			return LetterItem.Size.BIG;
		} else if (count > 1) {
			return LetterItem.Size.SMALL;
		} else {
			return LetterItem.Size.EMPTY;
		}
	}
}
