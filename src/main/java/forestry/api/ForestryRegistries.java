package forestry.api;

import com.mojang.serialization.MapCodec;

import forestry.api.apiculture.FlowerTypeType;
import forestry.api.apiculture.IBeeJubilance;
import forestry.api.apiculture.IFlowerType;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.api.core.FluidProductType;
import forestry.api.core.IFluidProduct;
import forestry.api.core.IProduct;
import forestry.api.core.ProductType;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.genetics.IMutationCondition;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.MutationConditionType;
import forestry.api.mail.IPostalCarrier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;

public class ForestryRegistries {
	public static final Registry<ICircuit> CIRCUIT = FabricRegistryBuilder.create(Keys.CIRCUIT_TYPE)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public static final Registry<IPostalCarrier> POSTAL_CARRIER = createPostalCarrierRegistry();

    private static Registry<IPostalCarrier> createPostalCarrierRegistry() {
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("forestrymail"))
            return FabricRegistryBuilder.create(Keys.POSTAL_CARRIER).attribute(RegistryAttribute.SYNCED).buildAndRegister();
        // No carriers exist without mail. Keep a frozen API view without adding an invalid empty built-in registry.
        return new net.minecraft.core.MappedRegistry<IPostalCarrier>(Keys.POSTAL_CARRIER, com.mojang.serialization.Lifecycle.stable()).freeze();
    }

	public static final Registry<ISpeciesType<?, ?>> SPECIES_TYPE = FabricRegistryBuilder.create(Keys.SPECIES_TYPE)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public static final Registry<MapCodec<? extends IBeeEffect>> BEE_EFFECT_TYPE = FabricRegistryBuilder.create(Keys.BEE_EFFECT_TYPE)
		.buildAndRegister();

	public static final Registry<MapCodec<? extends IBeeJubilance>> BEE_JUBILANCE_TYPE = FabricRegistryBuilder.create(Keys.BEE_JUBILANCE_TYPE)
		.buildAndRegister();

	public static final Registry<MutationConditionType<?>> MUTATION_CONDITION_TYPE = FabricRegistryBuilder.create(Keys.MUTATION_CONDITION_TYPE)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public static final Registry<ProductType<?>> PRODUCT_TYPE = FabricRegistryBuilder.create(Keys.PRODUCT_TYPE)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public static final Registry<FluidProductType<?>> FLUID_PRODUCT_TYPE = FabricRegistryBuilder.create(Keys.FLUID_PRODUCT_TYPE)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public static final Registry<FlowerTypeType<?>> FLOWER_TYPE_SERIALIZER = FabricRegistryBuilder.create(Keys.FLOWER_TYPE_SERIALIZER)
		.attribute(RegistryAttribute.SYNCED)
		.buildAndRegister();

	public static class Keys {
		public static final ResourceKey<Registry<ICircuit>> CIRCUIT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("circuit"));
		public static final ResourceKey<Registry<IPostalCarrier>> POSTAL_CARRIER = ResourceKey.createRegistryKey(ForestryConstants.forestry("postal_carrier"));
		public static final ResourceKey<Registry<ISpeciesType<?, ?>>> SPECIES_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("species_type"));
		public static final ResourceKey<Registry<MapCodec<? extends IBeeEffect>>> BEE_EFFECT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("bee_effect_type"));
		public static final ResourceKey<Registry<MapCodec<? extends IBeeJubilance>>> BEE_JUBILANCE_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("bee_jubilance_type"));
		public static final ResourceKey<Registry<MutationConditionType<?>>> MUTATION_CONDITION_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("mutation_condition_type"));
		public static final ResourceKey<Registry<ProductType<?>>> PRODUCT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("product_type"));
		public static final ResourceKey<Registry<FluidProductType<?>>> FLUID_PRODUCT_TYPE = ResourceKey.createRegistryKey(ForestryConstants.forestry("fluid_product_type"));
		public static final ResourceKey<Registry<FlowerTypeType<?>>> FLOWER_TYPE_SERIALIZER = ResourceKey.createRegistryKey(ForestryConstants.forestry("flower_type_serializer"));
	}
}
