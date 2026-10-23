package forestry.api;

import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import forestry.api.core.data.DataMapKey;
import forestry.api.core.data.DataMapSnapshot;

import forestry.api.core.machines.fuels.BiogasEngineFuel;
import forestry.api.core.machines.fuels.FermenterFuel;
import forestry.api.core.machines.fuels.MoistenerFuel;
import forestry.api.core.machines.fuels.PeatEngineFuel;
import forestry.api.core.machines.fuels.RainmakerSubstrate;

import static forestry.api.ForestryConstants.forestry;

/**
 * All data maps added by base Forestry.
 */
public class ForestryDataMaps {
    private static java.util.function.Supplier<DataMapSnapshot> current = () -> DataMapSnapshot.EMPTY;
    public static DataMapSnapshot current() { return current.get(); }
    public static void bindLookup(java.util.function.Supplier<DataMapSnapshot> lookup) { current = java.util.Objects.requireNonNull(lookup); }
    public static java.util.List<DataMapKey<?, ?>> all() {
        var result = new java.util.ArrayList<DataMapKey<?, ?>>(java.util.List.of(FERMENTER_FUELS, MOISTENER_FUELS, RAINMAKER_FUELS,
            BIOGAS_FUELS, COMBUSTION_FUELS, COMBUSTION_COOLANTS, PEAT_FUELS, FARM_FERTILIZERS, SWARMER_FEED));
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("forestrymail")) result.add(POSTAGE);
        return java.util.List.copyOf(result);
    }
	/**
	 * Add fuels for the Fermenter here (ex. fertilizer)
	 */
	public static final DataMapKey<Item, FermenterFuel> FERMENTER_FUELS = create("fermenter_fuels", Registries.ITEM, FermenterFuel.CODEC);

	/**
	 * Add resources for the Moistener here (ex. wheat)
	 */
	public static final DataMapKey<Item, MoistenerFuel> MOISTENER_FUELS = create("moistener_fuels", Registries.ITEM, MoistenerFuel.CODEC);

	/**
	 * Add substrates for the Rainmaker here
	 */
	public static final DataMapKey<Item, RainmakerSubstrate> RAINMAKER_FUELS = create("rainmaker_fuels", Registries.ITEM, RainmakerSubstrate.CODEC);

	/**
	 * Add fuels for the Biogas Engine here
	 */
	public static final DataMapKey<Fluid, BiogasEngineFuel> BIOGAS_FUELS = create("biogas_fuels", Registries.FLUID, BiogasEngineFuel.CODEC);

	/**
	 * Add fuels for the Combustion Engine here
	 */
	public static final DataMapKey<Fluid, BiogasEngineFuel> COMBUSTION_FUELS = create("combustion_fuels", Registries.FLUID, BiogasEngineFuel.CODEC);

	/**
	 * Add coolants for the Combustion Engine here. Only {@link BiogasEngineFuel#burnDuration()} and
	 * {@link BiogasEngineFuel#dissipationMultiplier()} are read for a coolant
	 */
	public static final DataMapKey<Fluid, BiogasEngineFuel> COMBUSTION_COOLANTS = create("combustion_coolants", Registries.FLUID, BiogasEngineFuel.CODEC);

	/**
	 * Add fuels for the Peat-fired Engine here
	 */
	public static final DataMapKey<Item, PeatEngineFuel> PEAT_FUELS = create("peat_fuels", Registries.ITEM, PeatEngineFuel.CODEC);

	/**
	 * Add fertilizers to use in Multiblock Farms and Planters. The value is the amount of fertilizer
	 * a single item is worth. Forestry's fertilizer is worth {@code 500}
	 */
	public static final DataMapKey<Item, Integer> FARM_FERTILIZERS = create("farm_fertilizers", Registries.ITEM, ExtraCodecs.POSITIVE_INT);

	/**
	 * Add items to be used as feed for the Alveary Swarmer. The value is the chance a swarm hive is
	 * created. For Royal Jelly, this is {@code 0.01} or 1%
	 */
	public static final DataMapKey<Item, Float> SWARMER_FEED = create("swarmer_feed", Registries.ITEM, Codec.floatRange(0f, 1f));

	/**
	 * The postage value of a stamp on a letter. Any item with postage>0 is usable as a stamp. Registered only when the mail jar is loaded.
	 */
	public static final DataMapKey<Item, Integer> POSTAGE = create("postage", Registries.ITEM, ExtraCodecs.POSITIVE_INT);

	private static <K, V> DataMapKey<K, V> create(String id, ResourceKey<Registry<K>> registry, Codec<V> codec) {
		return new DataMapKey<>(forestry(id), registry, codec);
	}
}
