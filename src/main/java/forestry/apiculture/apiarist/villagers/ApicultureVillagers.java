package forestry.apiculture.apiarist.villagers;

import com.google.common.collect.ImmutableSet;
import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.features.CoreBlocks;
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
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@FeatureProvider
public class ApicultureVillagers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.APICULTURE);

	private static final DeferredRegister<PoiType> POINTS_OF_INTEREST = REGISTRY.getRegistry(Registries.POINT_OF_INTEREST_TYPE);
	private static final DeferredRegister<VillagerProfession> PROFESSIONS = REGISTRY.getRegistry(Registries.VILLAGER_PROFESSION);

	public static final DeferredHolder<PoiType, PoiType> POI_ESCRITOIRE = POINTS_OF_INTEREST.register("escritoire", () -> new PoiType(Set.copyOf(CoreBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE).block().getStateDefinition().getPossibleStates()), 1, 1));
	public static final DeferredHolder<VillagerProfession, VillagerProfession> PROF_BEEKEEPER = PROFESSIONS.register("beekeeper", () -> {
		ResourceKey<PoiType> key = Objects.requireNonNull(POI_ESCRITOIRE.getKey());
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
