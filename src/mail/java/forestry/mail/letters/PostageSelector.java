package forestry.mail.letters;

import forestry.mail.MailFabricModule;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Picks the stamps that pay for a letter. Lifted out of the trade station so the arithmetic can be
 * read and tested on its own.
 *
 * <p>The three passes are the original greedy ones. They are not optimal and are not meant to be.
 */
public class PostageSelector {
	private static final int VIRTUAL_SUPPLY = 99;

	private static final Comparator<Denomination> CHEAPEST_FIRST = (left, right) -> {
		int byPostage = Integer.compare(left.postage(), right.postage());
		return byPostage != 0 ? byPostage : BuiltInRegistries.ITEM.getKey(left.item()).compareTo(BuiltInRegistries.ITEM.getKey(right.item()));
	};

	private PostageSelector() {
	}

	/**
	 * One kind of stamp and how many of it are on hand.
	 *
	 * @param item      The stamp item
	 * @param postage   The postage one of the item is worth
	 * @param available The number on hand
	 */
	public record Denomination(Item item, int postage, int available) {
	}

	/**
	 * @param stamps The stacks in a trade station's stamp slots
	 * @return The denominations those stacks make up, cheapest first
	 */
	public static List<Denomination> heldDenominations(Iterable<ItemStack> stamps) {
		Object2IntOpenHashMap<Item> held = new Object2IntOpenHashMap<>();

		for (ItemStack stamp : stamps) {
			if (stamp != null && PostageUtil.isStamp(stamp)) {
				held.addTo(stamp.getItem(), stamp.getCount());
			}
		}

		List<Denomination> denominations = new ArrayList<>(held.size());
		for (Object2IntMap.Entry<Item> entry : held.object2IntEntrySet()) {
			denominations.add(new Denomination(entry.getKey(), PostageUtil.getPostage(entry.getKey()), entry.getIntValue()));
		}

		denominations.sort(CHEAPEST_FIRST);
		return denominations;
	}

	/**
	 * A virtual trade station conjures its stamps rather than holding them, so it may only conjure the
	 * stamps Forestry ships. Reading the data map here would let it mint another mod's stamps.
	 *
	 * @return The denominations a virtual trade station pays with, cheapest first
	 */
	public static List<Denomination> virtualDenominations() {
		List<Denomination> denominations = new ArrayList<>(EnumStampDefinition.VALUES.length);

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			Item item = MailFabricModule.STAMPS.item(stamp);
			int postage = PostageUtil.getPostage(item);

			// A datapack can strip the item's postage entry, leaving it worth nothing
			if (postage > 0) {
				denominations.add(new Denomination(item, postage, VIRTUAL_SUPPLY));
			}
		}

		denominations.sort(CHEAPEST_FIRST);
		return denominations;
	}

	/**
	 * @param denominations The stamps on hand, cheapest first
	 * @param postageRequired The postage the letter needs
	 * @return The stamps to attach, which may fall short when the denominations cannot cover it
	 */
	public static List<ItemStack> select(List<Denomination> denominations, int postageRequired) {
		// A zero-postage denomination can only reach here through a caller building one directly, since
		// heldDenominations and virtualDenominations both filter it out. Dividing by it would crash
		List<Denomination> usable = new ArrayList<>(denominations.size());
		for (Denomination denomination : denominations) {
			if (denomination.postage() > 0) {
				usable.add(denomination);
			}
		}
		denominations = usable;

		int[] taken = new int[denominations.size()];
		int postageRemaining = postageRequired;

		// Largest first, taking as many of each as fit
		for (int i = denominations.size() - 1; i >= 0; i--) {
			if (postageRemaining <= 0) {
				break;
			}

			Denomination denomination = denominations.get(i);
			if (denomination.postage() > postageRemaining) {
				continue;
			}

			int num = Math.min(denomination.available(), postageRemaining / denomination.postage());
			taken[i] = num;
			postageRemaining -= num * denomination.postage();
		}

		// Use a larger stamp if exact change isn't available
		if (postageRemaining > 0) {
			for (int i = 0; i < denominations.size(); i++) {
				Denomination denomination = denominations.get(i);

				if (denomination.postage() >= postageRequired && denomination.available() > 0) {
					int[] single = new int[denominations.size()];
					single[i] = 1;
					return toStacks(denominations, single);
				}
			}
		}

		// If there isn't a single larger stamp we will just combine smaller ones, starting with the
		// higher values. This is totally disregarding whether there's a better solution or not
		if (postageRemaining > 0) {
			postageRemaining = postageRequired;
			taken = new int[denominations.size()];

			for (int i = denominations.size() - 1; i >= 0; i--) {
				Denomination denomination = denominations.get(i);

				int reqNum = Math.min((int) Math.ceil((double) postageRemaining / denomination.postage()), denomination.available());
				taken[i] = reqNum;
				postageRemaining -= reqNum * denomination.postage();

				if (postageRemaining <= 0) {
					break;
				}
			}
		}

		return toStacks(denominations, taken);
	}

	private static List<ItemStack> toStacks(List<Denomination> denominations, int[] taken) {
		List<ItemStack> stacks = new ArrayList<>();

		for (int i = 0; i < taken.length; i++) {
			if (taken[i] > 0) {
				stacks.add(new ItemStack(denominations.get(i).item(), taken[i]));
			}
		}

		return stacks;
	}
}
