package forestry.core.data;

import forestry.api.ForestryConstants;
import forestry.arboriculture.features.ArboricultureItems;
import forestry.arboriculture.loot.GrafterLootModifier;
import forestry.core.platform.loot.ConditionLootModifier;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.advancements.predicates.ItemPredicate.Builder.item;
import static net.minecraft.world.level.storage.loot.predicates.MatchTool.toolMatches;

/**
 * Data provider for the generation of global loot modifiers.
 * <p>
 * Currently the only modifier is the {@link ConditionLootModifier}
 */
public class ForestryLootModifierProvider extends GlobalLootModifierProvider {
	public ForestryLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider, ForestryConstants.MOD_ID);
	}

	@Override
	protected void start() {
		for (Map.Entry<ResourceKey<LootTable>, Collection<LootTableHelper.Entry>> mapEntry : LootTableHelper.getInstance().entries.asMap().entrySet()) {
			List<String> extensions = mapEntry.getValue().stream().map(entry -> entry.extension).toList();
			add(mapEntry.getKey().identifier().getPath(), new ConditionLootModifier(mapEntry.getKey().identifier(), extensions));
		}
		add("grafter", new GrafterLootModifier(new LootItemCondition[]{
			toolMatches(item().of(this.registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ArboricultureItems.GRAFTER.item())).or(toolMatches(item().of(this.registries.lookupOrThrow(net.minecraft.core.registries.Registries.ITEM), ArboricultureItems.PROVEN_GRAFTER.item()))).build()
		}));
	}
}
