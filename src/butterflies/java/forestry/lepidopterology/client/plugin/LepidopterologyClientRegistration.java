package forestry.lepidopterology.client.plugin;

import java.util.function.Consumer;

import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.lepidopterology.client.plugin.ButterflyAnalyzerPlugin;
import net.minecraft.resources.Identifier;
import forestry.api.client.plugin.IClientRegistration;

/**
 * Base Forestry's lepidopterology client registrations. Split out of
 * base Forestry's default plugin so the base artifact does not name
 * lepidopterology client types.
 */
public class LepidopterologyClientRegistration implements Consumer<IClientRegistration> {
	@Override
	public void accept(IClientRegistration client) {

		client.setAnalyzerPlugin(ForestrySpeciesTypes.BUTTERFLY, new ButterflyAnalyzerPlugin());

		// Register the default item/entity texture naming convention for every built-in butterfly species, so
		// ButterflyItemModel can bake a per-species model instead of falling back to the default (cabbage white)
		// for all of them. Sourced from the static compile-time id list, not the live/reloadable species map.
		for (Identifier speciesId : ForestryButterflySpecies.ALL) {
			registerDefaultButterflyTextures(client, speciesId);
		}
	}

	private static void registerDefaultButterflyTextures(IClientRegistration client, Identifier speciesId) {
		// Mirrors ButterflyClientManager#defaultTexturesFor's render-time fallback naming convention.
		String path = speciesId.getPath().replace("butterfly_", "");
		Identifier itemTexture = Identifier.fromNamespaceAndPath(speciesId.getNamespace(), "item/butterfly/" + path);
		Identifier entityTexture = Identifier.fromNamespaceAndPath(speciesId.getNamespace(), "textures/entity/butterfly/" + path + ".png");
		client.setButterflySprites(speciesId, itemTexture, entityTexture);
	}
}
