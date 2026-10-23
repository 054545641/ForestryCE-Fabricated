package forestry.mail.data;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.common.data.DataMapProvider;

import forestry.api.ForestryDataMaps;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumStampDefinition;

/**
 * Generates the postage every Forestry stamp is worth. The game merges a data map across every pack
 * that names it, so another mod adds its own stamps with a file of this shape and no dependency on
 * Forestry.
 */
public class MailDataMapProvider extends DataMapProvider {
	public MailDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
		super(output, lookup);
	}

	@Override
	protected void gather(HolderLookup.Provider provider) {
		var postage = builder(ForestryDataMaps.POSTAGE);

		for (EnumStampDefinition stamp : EnumStampDefinition.VALUES) {
			postage.add(MailItems.STAMPS.item(stamp).builtInRegistryHolder(), stamp.getPostage(), false);
		}
	}

	@Override
	public String getName() {
		return "Forestry Data Maps";
	}
}
