package forestry.apiculture.apiarist.villagers;

import com.google.common.collect.ImmutableSet;
import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.features.CoreUtilityBlocks;
import forestry.core.platform.block.BlockTypeCoreTesr;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import forestry.core.platform.registration.RegistryEntry;
import forestry.core.platform.registration.RegistrationBatch;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@FeatureProvider
public class ApicultureVillagers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	private static final RegistrationBatch<VillagerProfession> PROFESSIONS = REGISTRY.getRegistry(Registries.VILLAGER_PROFESSION);

	public static final PoiType POI_ESCRITOIRE = net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper.register(ForestryConstants.forestry("escritoire"), 1, 1, CoreUtilityBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE).block().getStateDefinition().getPossibleStates());
	public static final RegistryEntry<VillagerProfession, VillagerProfession> PROF_BEEKEEPER = PROFESSIONS.register("beekeeper", () -> {
		ResourceKey<PoiType> key = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, ForestryConstants.forestry("escritoire"));
		Predicate<Holder<PoiType>> jobSitePredicate = e -> e.is(key);
		return new VillagerProfession(Component.translatable("entity.forestry.villager.beekeeper"), jobSitePredicate, jobSitePredicate, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_LIBRARIAN, tradeSets("beekeeper", 4));
	});

	/** Trade set keys pointing at {@code data/forestry/trade_set/<profession>/level_<n>.json}. */
	private static Int2ObjectMap<ResourceKey<TradeSet>> tradeSets(String profession, int maxLevel) {
		Int2ObjectMap<ResourceKey<TradeSet>> map = new Int2ObjectOpenHashMap<>();
		for (int level = 1; level <= maxLevel; level++) {
			map.put(level, ResourceKey.create(Registries.TRADE_SET, ForestryConstants.forestry(profession + "/level_" + level)));
		}
		return map;
	}
}
