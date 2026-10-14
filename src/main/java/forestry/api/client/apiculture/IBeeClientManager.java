package forestry.api.client.apiculture;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * Tracks client-only concerns for bee species: model locations, and the ambient particles bees emit
 * while working.
 */
public interface IBeeClientManager {
	/**
	 * Retrieves the model location used to display a bee of the given species and life stage.
	 * To add a custom model for your bee, use {@link forestry.api.client.plugin.IClientRegistration#setCustomBeeModel}.
	 * If no custom model is set for the species, then the default model for the given life stage will be used instead, which is set by
	 * {@link forestry.api.client.plugin.IClientRegistration#setDefaultBeeModel}.
	 *
	 * @param stage     The life stage to retrieve the bee model for.
	 * @param speciesId The id of the bee species to retrieve the model for.
	 * @return The model location for the given species and life stage.
	 */
	Identifier getModelLocation(ILifeStage stage, Identifier speciesId);

	/**
	 * Retrieves the default model location used to display bees of the given life stage when no custom model is
	 * registered for their species, set by {@link forestry.api.client.plugin.IClientRegistration#setDefaultBeeModel}.
	 *
	 * @param stage The life stage to retrieve the default bee model for.
	 * @return The default model location for the given life stage.
	 */
	Identifier getDefaultModelLocation(ILifeStage stage);

	/**
	 * Retrieves every distinct model location used to display bees with the given life stage, including the
	 * default model and all custom models registered for that stage. Unlike {@link #getModelLocation}, this does
	 * not require knowledge of the (possibly datapack-driven) species list, since it is derived entirely from the
	 * models registered by {@link forestry.api.client.plugin.IClientRegistration}.
	 *
	 * @param stage The life stage to retrieve bee model locations for.
	 * @return All distinct model locations for the given life stage. (Ex. all drone models)
	 */
	Collection<Identifier> getAllModelLocations(ILifeStage stage);

	/**
	 * Spawns the ambient particles for a bee working in a hive or apiary. Client-side only.
	 *
	 * @param housing         The hive or apiary the bee resides in
	 * @param genome          The genome of the working bee
	 * @param flowerPositions The flower positions the bee is servicing
	 */
	void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions);
}
