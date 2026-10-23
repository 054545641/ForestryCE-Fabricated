package forestry.core.data;

import forestry.api.ForestryConstants;
import forestry.api.ForestryTags;
import forestry.arboriculture.features.ArboricultureItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

public class ForestryGiftLootTables implements LootTableSubProvider {
	public static final ResourceKey<LootTable> BEEKEEPER_GIFT = ResourceKey.create(Registries.LOOT_TABLE, ForestryConstants.forestry("gameplay/hero_of_the_village/beekeeper_gift"));
	public static final ResourceKey<LootTable> ARBORIST_GIFT = ResourceKey.create(Registries.LOOT_TABLE, ForestryConstants.forestry("gameplay/hero_of_the_village/arborist_gift"));

	public ForestryGiftLootTables(HolderLookup.Provider registries) {
	}

	@Override
	public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
		consumer.accept(BEEKEEPER_GIFT, LootTable.lootTable().withPool(LootPool.lootPool()
			.name("forestry_beekeeper_gift")
			.setRolls(ConstantValue.exactly(1))
			.add(TagEntry.expandTag(ForestryTags.Items.VILLAGE_COMBS)
				.apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
			.add(TagEntry.expandTag(ForestryTags.Items.SCOOPS).setWeight(5))
		));

		consumer.accept(ARBORIST_GIFT, LootTable.lootTable().withPool(LootPool.lootPool()
			.name("forestry_arborist_gift")
			.setRolls(ConstantValue.exactly(1))
			.add(TagEntry.expandTag(ForestryTags.Items.FORESTRY_FRUITS)
				.apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
			.add(LootItem.lootTableItem(ArboricultureItems.PROVEN_GRAFTER.item()).setWeight(4))
		));
	}
}
