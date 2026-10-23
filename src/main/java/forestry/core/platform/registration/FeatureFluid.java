package forestry.core.platform.registration;

import forestry.core.platform.client.ForestryColors;
import forestry.core.platform.fluids.BlockForestryFluid;
import forestry.core.platform.item.DrinkProperties;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class FeatureFluid extends ModFeature implements IFluidFeature {
	private final IBlockFeature<BlockForestryFluid, BlockItem> block;
	private final FluidProperties properties;

	private final RegistryEntry<Fluid, ? extends FlowingFluid> fluidObject;
	private final RegistryEntry<Fluid, ? extends FlowingFluid> flowingFluidObject;

	public FeatureFluid(Builder builder) {
		super(builder.moduleId, builder.identifier);
        this.properties = new FluidProperties(builder);
        RegistrationBatch<Fluid> fluidRegistry = builder.registry.getRegistry(Registries.FLUID);
        this.fluidObject = fluidRegistry.register(this.name, () -> new forestry.core.platform.fluids.ForestryFlowingFluid.Source(this));
        this.flowingFluidObject = fluidRegistry.register(this.name + "_flowing", () -> new forestry.core.platform.fluids.ForestryFlowingFluid.Flowing(this));
        this.block = builder.registry.block(properties -> new BlockForestryFluid(properties, this),
            net.minecraft.world.level.block.state.BlockBehaviour.Properties::of, null, "fluid_" + builder.identifier);

	}

	@Override
	public ResourceKey<? extends Registry<?>> getRegistry() {
		return Registries.FLUID;
	}

	@Override
	public IBlockFeature<BlockForestryFluid, BlockItem> fluidBlock() {
		return this.block;
	}

	@Override
	public FlowingFluid fluid() {
		return this.fluidObject.get();
	}

	@Override
	public FlowingFluid flowing() {
		return this.flowingFluidObject.get();
	}

	@Override
	public FluidProperties properties() {
		return this.properties;
	}

	public static class Builder {
		final FeatureRegistry registry;
		private final Identifier moduleId;
		final String identifier;

		int density = 1000;
		int viscosity = 1000;
		int temperature = 295;
		int particleColor = ForestryColors.WHITE;
		int flammability = 0;
		int luminosity = 0;
		boolean spreadsFire = false;
		@Nullable
		DrinkProperties properties = null;
		Supplier<Item> bucket = () -> Items.AIR;

		public Builder(FeatureRegistry registry, Identifier moduleId, String identifier) {
			this.registry = registry;
			this.moduleId = moduleId;
			this.identifier = identifier;
		}

		public Builder spreadsFire() {
			this.spreadsFire = true;
			return this;
		}

		public Builder luminosity(int luminosity) {
			this.luminosity = Mth.clamp(luminosity, 0, 15);
			return this;
		}

		public Builder flammability(int flammability) {
			this.flammability = flammability;
			return this;
		}

		public Builder density(int density) {
			this.density = density;
			return this;
		}

		public Builder viscosity(int viscosity) {
			this.viscosity = viscosity;
			return this;
		}

		// Temperatures are in Kelvin
		public Builder temperature(int temperature) {
			this.temperature = temperature;
			return this;
		}

		public Builder particleColor(int color) {
			this.particleColor = color;
			return this;
		}

		public Builder bucket(Supplier<Item> bucket) {
			this.bucket = bucket;
			return this;
		}

		public Builder drinkProperties(int healAmount, float saturationModifier, int maxItemUseDuration) {
			this.properties = new DrinkProperties(healAmount, saturationModifier, maxItemUseDuration);
			return this;
		}

		public FeatureFluid create() {
			FeatureFluid result = new FeatureFluid(this);
            this.registry.addFeature(result);
            return result;
		}
	}

}
