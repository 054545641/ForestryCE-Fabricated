package forestry.core.features;

import forestry.api.ForestryRegistries;
import forestry.api.core.genetics.MutationConditionType;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.engine.genetics.mutations.MutationConditionBiome;
import forestry.core.engine.genetics.mutations.MutationConditionCave;
import forestry.core.engine.genetics.mutations.MutationConditionDaytime;
import forestry.core.engine.genetics.mutations.MutationConditionHumidity;
import forestry.core.engine.genetics.mutations.MutationConditionRequiresResource;
import forestry.core.engine.genetics.mutations.MutationConditionTemperature;
import forestry.core.engine.genetics.mutations.MutationConditionTimeLimited;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

/**
 * The built-in mutation condition types, dispatched by {@code IMutationCondition#CODEC} in datapack mutation
 * recipes. Registered by base rather than by a genetics module: bees, trees and butterflies all share them.
 */
@FeatureProvider
public class CoreMutationConditionTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);

	public static final RegistrationBatch<MutationConditionType<?>> MUTATION_CONDITION_TYPES = REGISTRY.getRegistry(ForestryRegistries.Keys.MUTATION_CONDITION_TYPE);

	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionTemperature>> TEMPERATURE = MUTATION_CONDITION_TYPES.register("temperature", () -> MutationConditionTemperature.TYPE);
	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionHumidity>> HUMIDITY = MUTATION_CONDITION_TYPES.register("humidity", () -> MutationConditionHumidity.TYPE);
	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionBiome>> BIOME = MUTATION_CONDITION_TYPES.register("biome", () -> MutationConditionBiome.TYPE);
	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionDaytime>> DAYTIME = MUTATION_CONDITION_TYPES.register("daytime", () -> MutationConditionDaytime.TYPE);
	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionTimeLimited>> TIME_RANGE = MUTATION_CONDITION_TYPES.register("time_range", () -> MutationConditionTimeLimited.TYPE);
	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionRequiresResource>> REQUIRES_RESOURCE = MUTATION_CONDITION_TYPES.register("requires_resource", () -> MutationConditionRequiresResource.TYPE);
	public static final RegistryEntry<MutationConditionType<?>, MutationConditionType<MutationConditionCave>> CAVE = MUTATION_CONDITION_TYPES.register("cave", () -> MutationConditionCave.TYPE);
}
