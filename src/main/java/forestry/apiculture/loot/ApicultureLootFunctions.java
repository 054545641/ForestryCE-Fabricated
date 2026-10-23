package forestry.apiculture.loot;

import com.mojang.serialization.MapCodec;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

@FeatureProvider
public class ApicultureLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);
	private static final RegistrationBatch<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	public static final RegistryEntry<MapCodec<? extends LootItemFunction>, MapCodec<RandomBeeSpeciesFunction>> RANDOM_BEE_SPECIES = LOOT_FUNCTIONS.register("random_bee_species", () -> RandomBeeSpeciesFunction.CODEC);
}
