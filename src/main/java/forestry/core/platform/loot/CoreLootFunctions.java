package forestry.core.platform.loot;

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
public class CoreLootFunctions {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS = REGISTRY.getRegistry(Registries.LOOT_FUNCTION_TYPE);

	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<OrganismFunction>> ORGANISM = LOOT_FUNCTIONS.register("set_species_nbt", () -> OrganismFunction.CODEC);
	public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<RandomItemFromTagFunction>> RANDOM_ITEM_FROM_TAG = LOOT_FUNCTIONS.register("random_item_from_tag", () -> RandomItemFromTagFunction.CODEC);
}
