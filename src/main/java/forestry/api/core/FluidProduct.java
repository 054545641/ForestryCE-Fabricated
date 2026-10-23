package forestry.api.core;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import forestry.api.core.fluids.FluidVolume;
import forestry.api.core.fluids.FluidTemplate;
import forestry.api.core.fluids.FluidUnits;


/**
 * Default implementation of {@link IFluidProduct}: a fixed fluid output. Used by all of Forestry's own squeezer
 * recipes. Stores a template so fluid defaults are resolved only after components bind.
 *
 * @param template The fluid template this product always produces. Must not be empty.
 */
public record FluidProduct(FluidTemplate template) implements IFluidProduct {
	public FluidProduct {
		Preconditions.checkNotNull(template);
	}


	public FluidProduct(FluidVolume value) { this(FluidTemplate.from(value)); }
	public FluidVolume stack() { return this.template.create(); }

	public static final MapCodec<FluidProduct> MAP_CODEC = FluidTemplate.CODEC.fieldOf("stack").xmap(FluidProduct::new, FluidProduct::template);
	public static final StreamCodec<RegistryFriendlyByteBuf, FluidProduct> STREAM_CODEC = FluidTemplate.STREAM_CODEC.map(FluidProduct::new, FluidProduct::template);
	/**
	 * The default product type. The dispatch codec {@link IFluidProduct#CODEC} treats this type
	 * specially: products of this type serialize without a {@code "type"} key, and a missing {@code "type"} key on
	 * decode resolves back to it.
	 */
	public static final FluidProductType<FluidProduct> TYPE = new FluidProductType<>(MAP_CODEC, STREAM_CODEC);

	@Override
	public FluidVolume createFluidVolume() {
		return this.template.create();
	}

	@Override
	public FluidVolume createRandomFluidVolume(RandomSource random) {
		return this.template.create();
	}

	@Override
	public FluidProductType<?> type() {
		return TYPE;
	}

	public static FluidProduct of(FluidVolume stack) {
		return new FluidProduct(stack);
	}

	public static FluidProduct of(Fluid fluid, int amount) {
		return new FluidProduct(new FluidTemplate(fluid, FluidUnits.fromMilliBuckets(amount), net.minecraft.core.component.DataComponentPatch.EMPTY));
	}
}
