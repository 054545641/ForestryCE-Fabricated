package forestry.apiculture.client.plugin;

import java.util.function.Consumer;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.client.plugin.IClientRegistration;

/**
 * Base Forestry's apiculture client registrations. Split out of
 * base Forestry's default plugin so the base artifact does not name
 * apiculture client types.
 */
public class ApicultureClientRegistration implements Consumer<IClientRegistration> {
	@Override
	public void accept(IClientRegistration client) {

		client.setAnalyzerPlugin(ForestrySpeciesTypes.BEE, new BeeAnalyzerPlugin());

		client.setDefaultBeeModel(BeeLifeStage.DRONE, ForestryConstants.forestry("item/bee_drone_default"));
		client.setDefaultBeeModel(BeeLifeStage.PRINCESS, ForestryConstants.forestry("item/bee_princess_default"));
		client.setDefaultBeeModel(BeeLifeStage.QUEEN, ForestryConstants.forestry("item/bee_queen_default"));
		client.setDefaultBeeModel(BeeLifeStage.LARVAE, ForestryConstants.forestry("item/bee_larvae_default"));
		client.setCustomBeeModel(ForestryBeeSpecies.VANILLA, BeeLifeStage.DRONE, ForestryConstants.forestry("item/bee_drone_cube"));
		client.setCustomBeeModel(ForestryBeeSpecies.VANILLA, BeeLifeStage.PRINCESS, ForestryConstants.forestry("item/bee_princess_cube"));
		client.setCustomBeeModel(ForestryBeeSpecies.VANILLA, BeeLifeStage.QUEEN, ForestryConstants.forestry("item/bee_queen_cube"));
	}
}
