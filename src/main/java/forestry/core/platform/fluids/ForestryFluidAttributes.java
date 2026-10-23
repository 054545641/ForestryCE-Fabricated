package forestry.core.platform.fluids;

import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Transfer consumers and tooltips use the same physical metadata as Forestry's fluid definitions. */
public final class ForestryFluidAttributes {
    public static void initialize() {
        for (var fluid : ForestryFluids.values()) {
            var properties = fluid.getFeature().properties();
            var handler = new FluidVariantAttributeHandler() {
                @Override public Component getName(FluidVariant variant) { return Component.translatable("fluid_type.forestry." + fluid.getFeature().getName()); }
                @Override public int getLightEmission(FluidVariant variant) { return properties.luminosity; }
                @Override public int getTemperature(FluidVariant variant) { return properties.temperature; }
                @Override public int getViscosity(FluidVariant variant, Level level) { return properties.viscosity; }
                @Override public boolean isLighterThanAir(FluidVariant variant) { return properties.density < 0; }
            };
            FluidVariantAttributes.register(fluid.getFluid(), handler);
            FluidVariantAttributes.register(fluid.getFlowing(), handler);
        }
    }
}
