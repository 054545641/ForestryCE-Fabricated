package forestry.core.platform.fluids;

import forestry.api.ForestryConstants;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.client.ForestryColors;
import forestry.core.platform.item.DrinkProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import forestry.core.platform.registration.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.fluids.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;


import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.UnaryOperator;

@FeatureProvider
public enum ForestryFluids {
	BIO_ETHANOL(properties -> properties
		.particleColor(ForestryColors.color(255, 111, 0))
		.density(790)
		.viscosity(1000)
		.flammability(300)
		.spreadsFire()),
	BIOMASS(properties -> properties
		.particleColor(ForestryColors.color(100, 132, 41))
		.density(400)
		.viscosity(6560)
		.flammability(100)),
	GLASS(properties -> properties
		.particleColor(ForestryColors.color(164, 164, 164))
		.density(2400)
		.viscosity(10000)
		.flammability(0)
		.spreadsFire()
		.temperature(1400)),
	HONEY(properties -> properties
		.particleColor(ForestryColors.color(255, 196, 35))
		.density(1420)
		.viscosity(75600)
		.drinkProperties(2, 0.2f, 64)
	),
	ICE(properties -> properties
		.particleColor(ForestryColors.color(175, 242, 255))
		.density(520)
		.viscosity(1000)
		.temperature(265)),
	JUICE(properties -> properties
		.particleColor(ForestryColors.color(168, 201, 114))
		.drinkProperties(2, 0.2f, 32)
	),
	SEED_OIL(properties -> properties
		.particleColor(ForestryColors.color(255, 255, 168))
		.density(885)
		.viscosity(5000)
		.spreadsFire()
		.flammability(2)),
	SHORT_MEAD(properties -> properties
		.particleColor(ForestryColors.color(239, 154, 56))
		.density(1000)
		.viscosity(1200)
		.spreadsFire()
		.flammability(4)
		.drinkProperties(1, 0.2f, 32)
	),
	WAX(properties -> properties
		.particleColor(ForestryColors.color(255, 237, 140))
		.density(1420)
		.viscosity(500)
		.temperature(329)
	),
	EXPERIENCE(properties -> properties
		.particleColor(ForestryColors.color(133, 211, 84))
		.density(520)
		.viscosity(5000)
		.luminosity(15)
	);

	private static final Map<Identifier, ForestryFluids> BY_ID = new HashMap<>();

	static {
		for (ForestryFluids fluidDefinition : ForestryFluids.values()) {
			BY_ID.put(ForestryConstants.forestry(fluidDefinition.feature.getName()), fluidDefinition);
		}
	}

	private final Identifier tag;
	private final FeatureFluid feature;
	private final FeatureItem<BucketItem> bucket;

	ForestryFluids(UnaryOperator<FeatureFluid.Builder> properties) {
		IFeatureRegistry registry = ModFeatureRegistry.get(ForestryModuleIds.CORE);
		String fluidId = switch (name()) {
			case "GLASS" -> "liquid_glass";
			case "ICE" -> "crushed_ice";
			case "JUICE" -> "fruit_juice";
			case "BIO_ETHANOL" -> "ethanol";
			default -> name().toLowerCase(Locale.ENGLISH);
		};
		this.feature = properties.apply(registry
				.fluid(fluidId))
			.bucket(this::getBucket)
			.create();
		this.bucket = registry
			.item(itemProperties -> new BucketItem(getFluid(), itemProperties
					.craftRemainder(Items.BUCKET)
					.stacksTo(1)),
				"bucket_" + fluidId
			);
		this.tag = ForestryConstants.forestry(this.feature.getName());
	}

	public int getTemperature() {
		return this.feature.properties().temperature;
	}

	public final Identifier getTag() {
		return this.tag;
	}

	public FeatureFluid getFeature() {
		return this.feature;
	}

	public BucketItem getBucket() {
		return this.bucket.item();
	}
    public FeatureItem<BucketItem> getBucketFeature() { return this.bucket; }

	public final Fluid getFluid() {
		return this.feature.fluid();
	}

	public final Fluid getFlowing() {
		return this.feature.flowing();
	}

	public final FluidVolume getFluid(int mb) {
		Fluid fluid = getFluid();
		return new FluidVolume(FluidVariant.of(fluid), FluidUnits.fromMilliBuckets(mb));
	}

	public final int getParticleColor() {
		return this.feature.properties().particleColor;
	}

	public final boolean is(Fluid fluid) {
		return getFluid() == fluid;
	}

	public final boolean is(FluidVolume fluidStack) {
		return getFluid() == fluidStack.variant().getFluid();
	}

	public static boolean areEqual(@Nullable Fluid fluid, FluidVolume fluidStack) {
		return fluid == fluidStack.variant().getFluid();
	}

	@Nullable
	public static ForestryFluids getFluidDefinition(Fluid fluid) {
		return BY_ID.get(BuiltInRegistries.FLUID.getKey(fluid));
	}

	/**
	 * Get the properties for an ItemFluidContainerForestry before it gets registered.
	 */
	@Nullable
	public DrinkProperties getDrinkProperties() {
		return this.feature.properties().properties;
	}

	@SuppressWarnings("deprecation")
	public Holder<Fluid> holder() {
		return getFluid().builtInRegistryHolder();
	}
}
