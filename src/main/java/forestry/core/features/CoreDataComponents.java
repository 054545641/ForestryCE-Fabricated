package forestry.core.features;

import forestry.api.core.genetics.IGenome;
import forestry.core.content.backpacks.BackpackMode;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@FeatureProvider
public class CoreDataComponents {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.CORE);
	private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = REGISTRY.getRegistry(Registries.DATA_COMPONENT_TYPE);

	/**
	 * Generic fluid content component used by Forestry fluid-holding items (pipettes, capsules, cans, etc.).
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT =
		DATA_COMPONENT_TYPES.register(
			"fluid_content",
			() -> DataComponentType.<SimpleFluidContent>builder()
				.persistent(SimpleFluidContent.CODEC)
				.networkSynchronized(SimpleFluidContent.STREAM_CODEC)
				.build());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<IGenome>> GENOME =
		DATA_COMPONENT_TYPES.register(
			"genome",
			() -> DataComponentType.<IGenome>builder()
				.persistent(IGenome.CODEC)
				.networkSynchronized(ByteBufCodecs.fromCodec(IGenome.CODEC))
				.cacheEncoding()
				.build());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<IGenome>> MATE_GENOME =
		DATA_COMPONENT_TYPES.register(
			"mate_genome",
			() -> DataComponentType.<IGenome>builder()
				.persistent(IGenome.CODEC)
				.networkSynchronized(ByteBufCodecs.fromCodec(IGenome.CODEC))
				.cacheEncoding()
				.build());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ANALYZED =
		booleanComponent("analyzed");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEALTH =
		intComponent("health");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAX_HEALTH =
		intComponent("max_health");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BEE_PRISTINE =
		booleanComponent("bee_pristine");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BEE_GENERATION =
		intComponent("bee_generation");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ALYZER_CHARGES =
		intComponent("alyzer_charges");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ITEM_INVENTORY_UID =
		intComponent("item_inventory_uid");

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<BackpackMode>> BACKPACK_MODE =
		DATA_COMPONENT_TYPES.register("backpack_mode", BackpackMode::componentType);

	private static DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> booleanComponent(String id) {
		return DATA_COMPONENT_TYPES.register(id, () -> DataComponentType.<Boolean>builder()
			.persistent(com.mojang.serialization.Codec.BOOL)
			.networkSynchronized(ByteBufCodecs.BOOL)
			.build());
	}

	private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> intComponent(String id) {
		return DATA_COMPONENT_TYPES.register(id, () -> DataComponentType.<Integer>builder()
			.persistent(com.mojang.serialization.Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build());
	}

}
