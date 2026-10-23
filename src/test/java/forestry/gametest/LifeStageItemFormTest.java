package forestry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpeciesType;

/**
 * Guard for the life stage item form, which resolves through the item registry rather than holding
 * an item reference. A life stage whose id does not match a registered item silently yields
 * {@code minecraft:air} instead of throwing, so nothing fails at load and the wrong item reaches
 * tooltips, the analyzer and breeding output.
 *
 * <p>{@code SpeciesType} builds an {@code ImmutableMap<Item, ILifeStage>} from these item forms, so
 * TWO or more broken ids collide on the AIR key and throw at construction. One broken id produces no
 * duplicate and is invisible. This test covers that single-bad-id case.
 *
 * <p>Still valid once the jars split. A partial install never registers the absent module's
 * species type, so {@code getSpeciesTypes()} does not return it and its stages are never checked.
 *
 * Ex. BeeLifeStage.DRONE -> "forestry:drone_bee", not "forestry:bee_drone"
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class LifeStageItemFormTest {
	@GameTest(template = "empty")
	public static void everyLifeStageResolvesToARegisteredItem(GameTestHelper helper) {
		List<String> broken = new ArrayList<>();

		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			for (ILifeStage stage : type.getLifeStages()) {
				Item item = stage.getItemForm();
				if (item == Items.AIR) {
					broken.add(type.id() + " / " + stage.getSerializedName()
						+ "   (resolved to air; its item id matches nothing in the registry)");
				}
			}
		}

		if (!broken.isEmpty()) {
			helper.fail(broken.size() + " life stage(s) resolved to air:\n  " + String.join("\n  ", broken));
			return;
		}
		helper.succeed();
	}

	/**
	 * Asserts the round trip in the other direction: the item a life stage resolves to must be
	 * registered under exactly the id the stage asked for. Catches an id that happens to match some
	 * other mod's item.
	 */
	@GameTest(template = "empty")
	public static void everyLifeStageItemIsRegisteredUnderTheForestryNamespace(GameTestHelper helper) {
		List<String> wrong = new ArrayList<>();

		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			for (ILifeStage stage : type.getLifeStages()) {
				Item item = stage.getItemForm();
				if (item == Items.AIR) {
					continue;
				}
				var id = BuiltInRegistries.ITEM.getKey(item);
				if (!ForestryConstants.MOD_ID.equals(id.getNamespace())) {
					wrong.add(type.id() + " / " + stage.getSerializedName() + " -> " + id);
				}
			}
		}

		if (!wrong.isEmpty()) {
			helper.fail(wrong.size() + " life stage item(s) are not in the forestry namespace:\n  "
				+ String.join("\n  ", wrong));
			return;
		}
		helper.succeed();
	}
}
