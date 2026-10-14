package forestry.api.core.genetics.alleles;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import net.minecraft.util.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;


import javax.annotation.Nullable;

/**
 * The single, generic chromosome implementation. Replaces the former float/integer/boolean/value/registry subtypes.
 * Data chromosomes have a {@code null} resolver; reference chromosomes store a {@link Identifier} and resolve
 * it to a behavior object lazily via the resolver.
 */
public final class Chromosome<V> implements IChromosome<V> {
	private final Identifier id;
	private final Codec<V> valueCodec;
	private final StreamCodec<RegistryFriendlyByteBuf, V> valueStreamCodec;
	private final String chromosomeTranslationKey;
	private final Function<V, String> naming;
	@Nullable
	private final IReferenceResolver<?> resolver;

	public Chromosome(Identifier id, Codec<V> valueCodec, Function<V, String> naming, @Nullable IReferenceResolver<?> resolver) {
		this.id = id;
		this.valueCodec = valueCodec;
		this.valueStreamCodec = ByteBufCodecs.fromCodecWithRegistries(valueCodec);
		this.chromosomeTranslationKey = Util.makeDescriptionId("chromosome", id);
		this.naming = naming;
		this.resolver = resolver;
	}

	@Override
	public Identifier id() {
		return this.id;
	}

	@Override
	public Codec<V> valueCodec() {
		return this.valueCodec;
	}

	@Override
	public StreamCodec<RegistryFriendlyByteBuf, V> valueStreamCodec() {
		return this.valueStreamCodec;
	}

	@Override
	public String chromosomeTranslationKey() {
		return this.chromosomeTranslationKey;
	}

	@Override
	public String translationKey(V value) {
		return this.naming.apply(value);
	}

	@Nullable
	@Override
	public IReferenceResolver<?> resolver() {
		return this.resolver;
	}

	@Override
	public String toString() {
		return "Chromosome[" + this.id + ']';
	}
}
