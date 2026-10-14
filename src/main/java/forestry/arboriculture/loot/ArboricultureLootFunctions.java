package forestry.arboriculture.loot;

import com.mojang.serialization.MapCodec;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@FeatureProvider
public class ArboricultureLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);
	private static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	// name matches the one generated loot tables reference
	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<CountBlockFunction>> COUNT = LOOT_FUNCTIONS.register("count_from_block", () -> CountBlockFunction.CODEC);
	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<RandomWoodFunction>> RANDOM_WOOD = LOOT_FUNCTIONS.register("random_wood", () -> RandomWoodFunction.CODEC);
	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<RandomTreeSpeciesFunction>> RANDOM_TREE_SPECIES = LOOT_FUNCTIONS.register("random_tree_species", () -> RandomTreeSpeciesFunction.CODEC);
}
