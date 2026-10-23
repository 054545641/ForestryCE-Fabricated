package forestry.lepidopterology.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import forestry.lepidopterology.features.LepidopterologyItems;

/**
 * Generates the {@code neoforge:compostables} entries the butterflies jar contributes. The game merges
 * a data map across every pack that names it, so core ships the rest of {@code compostables} from its
 * own file.
 */
public class LepidopterologyDataMapProvider extends DataMapProvider {
	public LepidopterologyDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(packOutput, lookupProvider);
	}

	@Override
	protected void gather(HolderLookup.Provider provider) {
		Builder<Compostable, Item> composts = builder(NeoForgeDataMaps.COMPOSTABLES);

		composts.add(BuiltInRegistries.ITEM.getKey(LepidopterologyItems.COCOON_GE.item()), new Compostable(0.3f), false);
	}

	@Override
	public String getName() {
		return "Forestry Data Maps";
	}
}
