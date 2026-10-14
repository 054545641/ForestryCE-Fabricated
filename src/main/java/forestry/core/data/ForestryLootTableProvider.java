package forestry.core.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ForestryLootTableProvider extends LootTableProvider {
	/**
	 * @param output         The root core's loot tables are written to
	 * @param lookupProvider The lookup the sub-providers resolve registry entries through
	 * @param contentOwned   The ids the content jars write loot for, which core skips
	 */
	public ForestryLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, Set<Identifier> contentOwned) {
		super(output, Set.of(), List.of(
			new SubProviderEntry(registries -> new ForestryBlockLootTables(registries, contentOwned), LootContextParamSets.BLOCK),
			new SubProviderEntry(ForestryChestLootTables::new, LootContextParamSets.CHEST),
			new SubProviderEntry(ForestryGiftLootTables::new, LootContextParamSets.GIFT)
		), lookupProvider);
	}
}
