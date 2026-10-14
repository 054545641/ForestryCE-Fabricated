package forestry.arboriculture.client.plugin;

import java.util.function.Consumer;

import forestry.api.ForestryConstants;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.client.arboriculture.ForestryLeafSprites;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.arboriculture.client.BiomeLeafTint;
import forestry.arboriculture.client.FixedLeafTint;
import forestry.arboriculture.client.plugin.TreeAnalyzerPlugin;
import net.minecraft.resources.Identifier;
import forestry.api.client.plugin.IClientRegistration;

/**
 * Base Forestry's arboriculture client registrations. Split out of
 * base Forestry's default plugin so the base artifact does not name
 * arboriculture client types.
 */
public class ArboricultureClientRegistration implements Consumer<IClientRegistration> {
	@Override
	public void accept(IClientRegistration client) {

		client.setAnalyzerPlugin(ForestrySpeciesTypes.TREE, new TreeAnalyzerPlugin());

		// Vanilla sapling models
		registerSapling(client, "minecraft", ForestryTreeSpecies.OAK);
		registerSapling(client, "minecraft", ForestryTreeSpecies.DARK_OAK);
		registerSapling(client, "minecraft", ForestryTreeSpecies.BIRCH);
		registerSapling(client, "minecraft", ForestryTreeSpecies.ACACIA_VANILLA);
		registerSapling(client, "minecraft", ForestryTreeSpecies.SPRUCE);
		registerSapling(client, "minecraft", ForestryTreeSpecies.JUNGLE);
		registerSapling(client, "minecraft", ForestryTreeSpecies.CHERRY_VANILLA);

		// Vanilla leaf sprites
		client.setLeafSprite(ForestryTreeSpecies.OAK, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.DARK_OAK, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.BIRCH, ForestryLeafSprites.BIRCH);
		client.setLeafSprite(ForestryTreeSpecies.ACACIA_VANILLA, ForestryLeafSprites.ACACIA);
		client.setLeafSprite(ForestryTreeSpecies.SPRUCE, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.JUNGLE, ForestryLeafSprites.JUNGLE);
		client.setLeafSprite(ForestryTreeSpecies.CHERRY_VANILLA, ForestryLeafSprites.CHERRY);

		// Forestry leaf sprites
		client.setLeafSprite(ForestryTreeSpecies.LIME, ForestryLeafSprites.BIRCH);
		client.setLeafSprite(ForestryTreeSpecies.WALNUT, ForestryLeafSprites.ACACIA);
		client.setLeafSprite(ForestryTreeSpecies.CHESTNUT, ForestryLeafSprites.BIRCH);
		client.setLeafSprite(ForestryTreeSpecies.SOUR_CHERRY, ForestryLeafSprites.BIRCH);
		client.setLeafSprite(ForestryTreeSpecies.LEMON, ForestryLeafSprites.AZALEA);
		client.setLeafSprite(ForestryTreeSpecies.PLUM, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.MAPLE, ForestryLeafSprites.MAPLE);
		client.setLeafSprite(ForestryTreeSpecies.LARCH, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.PINE, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.SEQUOIA, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.GIANT_SEQUOIA, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.TEAK, ForestryLeafSprites.JUNGLE);
		client.setLeafSprite(ForestryTreeSpecies.IPE, ForestryLeafSprites.IPE);
		client.setLeafSprite(ForestryTreeSpecies.KAPOK, ForestryLeafSprites.JUNGLE);
		client.setLeafSprite(ForestryTreeSpecies.EBONY, ForestryLeafSprites.JUNGLE);
		client.setLeafSprite(ForestryTreeSpecies.ZEBRANO, ForestryLeafSprites.JUNGLE);
		client.setLeafSprite(ForestryTreeSpecies.MAHOGANY, ForestryLeafSprites.JUNGLE);
		client.setLeafSprite(ForestryTreeSpecies.CAMELTHORN, ForestryLeafSprites.ACACIA);
		client.setLeafSprite(ForestryTreeSpecies.PADAUK, ForestryLeafSprites.ACACIA);
		client.setLeafSprite(ForestryTreeSpecies.BALSA, ForestryLeafSprites.ACACIA);
		client.setLeafSprite(ForestryTreeSpecies.COCOBOLO, ForestryLeafSprites.MANGROVE);
		client.setLeafSprite(ForestryTreeSpecies.WENGE, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.BAOBAB, ForestryLeafSprites.ACACIA);
		client.setLeafSprite(ForestryTreeSpecies.MAHOE, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.WILLOW, ForestryLeafSprites.WILLOW);
		client.setLeafSprite(ForestryTreeSpecies.GREENHEART, ForestryLeafSprites.MANGROVE);
		client.setLeafSprite(ForestryTreeSpecies.PAPAYA, ForestryLeafSprites.PALM);
		client.setLeafSprite(ForestryTreeSpecies.DATE, ForestryLeafSprites.PALM);
		client.setLeafSprite(ForestryTreeSpecies.POPLAR, ForestryLeafSprites.BIRCH);
		client.setLeafSprite(ForestryTreeSpecies.ELM, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.FIR, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.COCONUT, ForestryLeafSprites.PALM);
		client.setLeafSprite(ForestryTreeSpecies.BEECH, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.FEIJOA, ForestryLeafSprites.AZALEA);
		client.setLeafSprite(ForestryTreeSpecies.DOGWOOD, ForestryLeafSprites.DOGWOOD);
		client.setLeafSprite(ForestryTreeSpecies.GINKGO, ForestryLeafSprites.GINKGO);
		client.setLeafSprite(ForestryTreeSpecies.JACARANDA, ForestryLeafSprites.JACARANDA);
		client.setLeafSprite(ForestryTreeSpecies.PEWEN, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.MACROCARPA, ForestryLeafSprites.SPRUCE);
		client.setLeafSprite(ForestryTreeSpecies.OLIVE, ForestryLeafSprites.WILLOW);
		client.setLeafSprite(ForestryTreeSpecies.ORANGE, ForestryLeafSprites.AZALEA);
		client.setLeafSprite(ForestryTreeSpecies.PEAR, ForestryLeafSprites.OAK);
		client.setLeafSprite(ForestryTreeSpecies.KAURI, ForestryLeafSprites.SPRUCE);

		// Vanilla leaf tints
		client.setLeafTint(ForestryTreeSpecies.OAK, BiomeLeafTint.DEFAULT);
		client.setLeafTint(ForestryTreeSpecies.DARK_OAK, BiomeLeafTint.DEFAULT);
		client.setLeafTint(ForestryTreeSpecies.JUNGLE, BiomeLeafTint.DEFAULT);
		client.setLeafTint(ForestryTreeSpecies.ACACIA_VANILLA, BiomeLeafTint.DEFAULT);
		client.setLeafTint(ForestryTreeSpecies.CHERRY_VANILLA, FixedLeafTint.NONE);

		// Modded leaf tints
		client.setLeafTint(ForestryTreeSpecies.DOGWOOD, FixedLeafTint.NONE);
		client.setLeafTint(ForestryTreeSpecies.JACARANDA, FixedLeafTint.NONE);
		client.setLeafTint(ForestryTreeSpecies.IPE, FixedLeafTint.NONE);
	}

	private static void registerSapling(IClientRegistration registration, String modId, Identifier speciesId) {
		// add "_sapling" (no longer need to remove "tree_" prefix)
		String path = speciesId.getPath() + "_sapling";

		Identifier blockModel = Identifier.fromNamespaceAndPath(modId, "block/" + path);
		Identifier itemModel = Identifier.fromNamespaceAndPath(modId, "item/" + path);
		registration.setSaplingModel(speciesId, blockModel, itemModel);
	}
}
