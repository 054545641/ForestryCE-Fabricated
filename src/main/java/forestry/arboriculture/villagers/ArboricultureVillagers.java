package forestry.arboriculture.villagers;

import com.google.common.collect.ImmutableSet;
import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.features.CoreBlocks;
import forestry.core.platform.block.NaturalistChestBlockType;
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
public class ArboricultureVillagers {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ARBORICULTURE);

	private static final DeferredRegister<PoiType> POINTS_OF_INTEREST = REGISTRY.getRegistry(Registries.POINT_OF_INTEREST_TYPE);
	private static final DeferredRegister<VillagerProfession> PROFESSIONS = REGISTRY.getRegistry(Registries.VILLAGER_PROFESSION);

	public static final DeferredHolder<PoiType, PoiType> POI_TREE_CHEST = POINTS_OF_INTEREST.register("tree_chest", () -> new PoiType(Set.copyOf(CoreBlocks.NATURALIST_CHEST.get(NaturalistChestBlockType.ARBORIST_CHEST).block().getStateDefinition().getPossibleStates()), 1, 1));
	public static final DeferredHolder<VillagerProfession, VillagerProfession> ARBORIST = PROFESSIONS.register("arborist", () -> {
		ResourceKey<PoiType> key = Objects.requireNonNull(POI_TREE_CHEST.getKey());
		Predicate<Holder<PoiType>> jobSitePredicate = poi -> poi.is(key);
		return new VillagerProfession(Component.translatable("entity.forestry.villager.arborist"), jobSitePredicate, jobSitePredicate, ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_FISHERMAN, tradeSets("arborist", 4));
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
