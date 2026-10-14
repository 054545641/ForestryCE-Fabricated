package forestry.agriculture.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import net.minecraft.data.advancements.AdvancementProvider;

import forestry.agriculture.features.MinifarmBlocks;
import forestry.agriculture.features.MultifarmBlocks;
import forestry.agriculture.multifarm.blocks.MultifarmBlockType;
import forestry.agriculture.multifarm.blocks.MultifarmMaterialType;
import forestry.agriculture.minifarm.blocks.MinifarmBlockType;
import forestry.core.data.ForestryAdvancements;

/**
 * The two advancements that ask for farms jar content. Both hang under core's get_fabricator, which
 * the game resolves once the two packs are loaded together.
 */
// Deviation from 1.20.1: 1.20.1 wrote all 57 advancements from one provider. The planters and the
// multifarm are farms jar content, which core's provider cannot see, so these two write here
public class AgricultureAdvancementProvider extends AdvancementProvider {
	public AgricultureAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, List.of(new FarmAdvancements()));
	}

	private static class FarmAdvancements implements net.minecraft.data.advancements.AdvancementSubProvider {
		@Override
		public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> writer) {
			AdvancementHolder fabricator = ForestryAdvancements.reference("get_fabricator");

			List<Item> planters = new ArrayList<>();
			for (BlockItem planter : MinifarmBlocks.MANAGED_PLANTER.getItems()) {
				planters.add(planter.asItem());
			}
			for (BlockItem planter : MinifarmBlocks.MANUAL_PLANTER.getItems()) {
				planters.add(planter.asItem());
			}

			// Farming Simulator
			AdvancementHolder farmingSimulator = ForestryAdvancements.add(writer, "farming_simulator",
				MinifarmBlocks.MANAGED_PLANTER.stack(MinifarmBlockType.ARBORETUM),
				fabricator,
				InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), planters.toArray(Item[]::new))));

			// Feed The World
			// Granted manually via ContainerFarm
			ForestryAdvancements.add(writer, "feed_the_world",
				MultifarmBlocks.FARM.stack(MultifarmBlockType.PLAIN, MultifarmMaterialType.STONE_BRICK),
				farmingSimulator,
				CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()),
				AdvancementType.GOAL, false);
		}
	}
}
