package forestry.api.core.fluids;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** A recipe declaration may be constructed before the fluid's default components are bound. */
public record FluidTemplate(Fluid fluid, long amount, DataComponentPatch components) {
    public FluidTemplate {
        java.util.Objects.requireNonNull(fluid);
        java.util.Objects.requireNonNull(components);
        if (fluid == Fluids.EMPTY || amount <= 0) throw new IllegalArgumentException("Empty fluid product");
    }
    public static FluidTemplate from(FluidVolume value) { return new FluidTemplate(value.variant().getFluid(), value.amount(), value.variant().getComponentsPatch()); }
    public FluidVolume create() { return new FluidVolume(FluidVariant.of(fluid, components), amount); }
    public static final Codec<FluidTemplate> CODEC = FluidVolume.CODEC.comapFlatMap(value -> value.isEmpty()
        ? com.mojang.serialization.DataResult.error(() -> "Empty fluid product") : com.mojang.serialization.DataResult.success(from(value)), FluidTemplate::create);
    public static final StreamCodec<RegistryFriendlyByteBuf, FluidTemplate> STREAM_CODEC = FluidVolume.STREAM_CODEC.map(FluidTemplate::from, FluidTemplate::create);
}
