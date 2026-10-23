package forestry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.ForestryDataMaps;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.PostageUtil;

/**
 * Guard for the postage data map. The data map is what lets another mod declare a stamp with one JSON
 * file and no dependency on the mail jar, so a data map that fails to register, or a generated file
 * that fails to load, silently turns every Forestry stamp into a worthless item.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class PostageDataMapTest {
	@GameTest(template = "empty")
	public static void everyForestryStampCarriesItsPostage(GameTestHelper helper) {
		List<String> broken = new ArrayList<>();

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			Item item = MailItems.STAMPS.item(stamp);
			Integer postage = item.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE);
			int expected = stamp.getPostage();

			if (postage == null) {
				broken.add(stamp.getSerializedName() + " has no postage entry at all");
			} else if (postage != expected) {
				broken.add(stamp.getSerializedName() + " is worth " + postage + " instead of " + expected);
			}
		}

		if (!broken.isEmpty()) {
			helper.fail(broken.size() + " stamp(s) did not resolve their postage:\n  " + String.join("\n  ", broken));
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void nonStampsCarryNoPostage(GameTestHelper helper) {
		helper.assertTrue(Items.PAPER.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE) == null,
			"Paper resolved a postage value, so the data map is matching items it should not");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void postageUtilReadsTheDataMap(GameTestHelper helper) {
		ItemStack tenner = MailItems.STAMPS.stack(EnumStampDefinition.P_10, 3);

		assertEquals(helper, PostageUtil.getPostage(tenner), 10, "postage of a 10n stamp");
		helper.assertTrue(PostageUtil.isStamp(tenner), "A 10n stamp did not read as a stamp");
		assertEquals(helper, PostageUtil.getPostage(new ItemStack(Items.PAPER)), 0, "postage of paper");
		helper.assertFalse(PostageUtil.isStamp(new ItemStack(Items.PAPER)), "Paper read as a stamp");
		assertEquals(helper, PostageUtil.getPostage(ItemStack.EMPTY), 0, "postage of an empty stack");
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void sumPostageMultipliesByCount(GameTestHelper helper) {
		List<ItemStack> stamps = List.of(
			MailItems.STAMPS.stack(EnumStampDefinition.P_10, 3),
			MailItems.STAMPS.stack(EnumStampDefinition.P_1, 4),
			new ItemStack(Items.PAPER, 64),
			ItemStack.EMPTY);

		// 10*3 + 1*4, and neither the paper nor the empty stack contributes
		assertEquals(helper, PostageUtil.sumPostage(stamps), 34, "summed postage");
		helper.succeed();
	}

	private static void assertEquals(GameTestHelper helper, int actual, int expected, String what) {
		helper.assertTrue(actual == expected, what + " was " + actual + " instead of " + expected);
	}
}
