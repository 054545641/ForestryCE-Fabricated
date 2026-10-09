package forestry.gametest;

import forestry.api.ForestryConstants;
import forestry.core.features.CoreItems;
import forestry.core.manual.ManualAccess;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class ManualAccessTest {
	// passes in both boot configurations: the wiring must track whether patchouli actually loaded
	@GameTest(template = "empty")
	public static void manualAccessMatchesPatchouliPresence(GameTestHelper helper) {
		boolean patchouliLoaded = ModList.get().isLoaded("patchouli");
		helper.assertTrue(ManualAccess.get().isAvailable() == patchouliLoaded,
			"ManualAccess availability " + ManualAccess.get().isAvailable() + " disagrees with ModList patchouli=" + patchouliLoaded);
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void manualUseWithoutProviderIsSafe(GameTestHelper helper) {
		if (ManualAccess.get().isAvailable()) {
			// the available path opens the book GUI, which needs a real client and stays manual QA
			helper.succeed();
			return;
		}

		int[] messages = {0};
		int[] sounds = {0};
		FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ManualTest")) {
			@Override
			public void displayClientMessage(Component message, boolean actionBar) {
				messages[0]++;
				helper.assertTrue(actionBar, "missing-provider hint must use the action bar");
				helper.assertTrue(message.getContents() instanceof TranslatableContents contents
					&& contents.getKey().equals("item.forestry.foresters_manual.missing_patchouli"),
					"missing-provider hint must be translatable");
			}

			@Override
			public void playSound(SoundEvent sound, float volume, float pitch) {
				sounds[0]++;
			}
		};
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CoreItems.FORESTERS_MANUAL.item()));

		// a missing provider must not throw NoClassDefFoundError, consume the item, or fail the use
		InteractionResultHolder<ItemStack> result = CoreItems.FORESTERS_MANUAL.item()
			.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.assertTrue(result.getResult() == InteractionResult.SUCCESS,
			"expected SUCCESS using the manual without patchouli, got " + result.getResult());
		helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
			"manual stack size changed without patchouli");
		helper.assertTrue(messages[0] == 1, "expected exactly one missing-provider hint per use");
		helper.assertTrue(sounds[0] == 0, "missing provider must not play a success sound");
		helper.succeed();
	}
}
