package forestry.api.core.fluids;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Immutable fluid value. Amount is always in Fabric droplets, including fractional mB. */
public record FluidVolume(FluidVariant variant, long amount) {
    public static final FluidVolume EMPTY = new FluidVolume(FluidVariant.blank(), 0);

    public FluidVolume {
        java.util.Objects.requireNonNull(variant);
        if (amount < 0 || (amount > 0 && variant.isBlank())) throw new IllegalArgumentException("Invalid fluid volume");
        if (amount == 0) variant = FluidVariant.blank();
    }

    public boolean isEmpty() { return amount == 0; }
    public Fluid getFluid() { return variant.getFluid(); }
    public long getAmount() { return amount; }
    public net.minecraft.core.Holder<Fluid> typeHolder() { return getFluid().builtInRegistryHolder(); }
    public FluidVolume copy() { return this; }
    public FluidVolume copyWithAmount(long droplets) { return withAmount(droplets); }
    public static boolean isSameFluidSameComponents(FluidVolume a, FluidVolume b) { return a.variant.equals(b.variant); }
    public static boolean matches(FluidVolume a, FluidVolume b) { return a.equals(b); }
    public FluidVolume withAmount(long amount) { return new FluidVolume(variant, amount); }
    public static FluidVolume milliBuckets(Fluid fluid, long amount) {
        return new FluidVolume(FluidVariant.of(fluid), FluidUnits.fromMilliBuckets(amount));
    }

    private record Encoded(int schema, String unit, Fluid fluid, long amount, DataComponentPatch components) {}
    private static final Codec<Encoded> ENCODED = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("schema", 1).forGetter(Encoded::schema),
        Codec.STRING.optionalFieldOf("unit", "mB").forGetter(Encoded::unit),
        BuiltInRegistries.FLUID.byNameCodec().optionalFieldOf("id", Fluids.EMPTY).forGetter(Encoded::fluid),
        Codec.LONG.optionalFieldOf("amount", 0L).forGetter(Encoded::amount),
        DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(Encoded::components)
    ).apply(instance, Encoded::new));

    /** Reads the old id/amount/components form as mB; writes an explicitly versioned droplet form. */
    public static final Codec<FluidVolume> CODEC = ENCODED.flatXmap(encoded -> {
        try {
            long droplets;
            if (encoded.schema == 1 && encoded.unit.equals("mB")) droplets = FluidUnits.fromMilliBuckets(encoded.amount);
            else if (encoded.schema == 2 && encoded.unit.equals("droplet")) droplets = encoded.amount;
            else return DataResult.error(() -> "Unsupported fluid schema/unit: " + encoded.schema + "/" + encoded.unit);
            return DataResult.success(new FluidVolume(FluidVariant.of(encoded.fluid, encoded.components), droplets));
        } catch (IllegalArgumentException | ArithmeticException error) {
            return DataResult.error(() -> "Invalid fluid data: " + error.getMessage());
        }
    }, volume -> DataResult.success(new Encoded(2, "droplet", volume.variant.getFluid(), volume.amount, volume.variant.getComponentsPatch())));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidVolume> STREAM_CODEC = StreamCodec.of((buffer, volume) -> {
        buffer.writeVarLong(volume.amount);
        if (!volume.isEmpty()) FluidVariant.PACKET_CODEC.encode(buffer, volume.variant);
    }, buffer -> {
        long amount = buffer.readVarLong();
        if (amount == 0) return EMPTY;
        if (amount < 0) throw new IllegalArgumentException("Negative fluid amount in packet");
        return new FluidVolume(FluidVariant.PACKET_CODEC.decode(buffer), amount);
    });
}
