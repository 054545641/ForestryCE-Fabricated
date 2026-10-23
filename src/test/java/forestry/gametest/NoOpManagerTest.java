package forestry.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.apiimpl.fake.FakeFarmingManager;

/**
 * Farms is the only optional jar that owns an api manager, so the farming no-op is the only one left.
 * A run with farms installed never reaches it, which is why this constructs it directly. What it
 * protects is the contract an addon relies on when farms is absent: non-null returns and empty
 * results rather than a throw.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class NoOpManagerTest {
	@GameTest(template = "empty")
	public static void noOpReportsNotLoaded(GameTestHelper helper) {
		if (FakeFarmingManager.INSTANCE.isLoaded()) {
			helper.fail("The no-op farming manager reported itself loaded");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void noOpFarmingManagerDegrades(GameTestHelper helper) {
		Identifier any = ForestryConstants.forestry("wheat");

		if (FakeFarmingManager.INSTANCE.getFarmType(any) != null) {
			helper.fail("No-op farming manager returned a farm type");
			return;
		}
		if (!FakeFarmingManager.INSTANCE.getFarmables(any).isEmpty()) {
			helper.fail("No-op farming manager returned farmables");
			return;
		}
		if (FakeFarmingManager.INSTANCE.getFertilizeValue(new ItemStack(Items.BONE_MEAL)) != 0) {
			helper.fail("No-op farming manager valued a fertilizer");
			return;
		}
		helper.succeed();
	}
}
