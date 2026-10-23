package forestry.core.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Generates the loot table pools for a content jar, delegating to that jar's block sub-provider. Every
 * jar wrapped its sub-provider in the same three lines, so the sub-provider is a parameter here.
 */
public class JarLootTableProvider extends LootTableProvider {
	/**
	 * @param output The root this jar's loot tables are written to
	 * @param lookup The lookup the sub-provider resolves registry entries through
	 * @param blocks The block sub-provider that builds this jar's tables
	 */
	public JarLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, Function<HolderLookup.Provider, LootTableSubProvider> blocks) {
		super(output, Set.of(), List.of(
			new SubProviderEntry(blocks, LootContextParamSets.BLOCK)
		), lookup);
	}
}
