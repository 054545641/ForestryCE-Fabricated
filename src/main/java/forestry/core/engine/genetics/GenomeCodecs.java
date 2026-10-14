package forestry.core.engine.genetics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AlleleOverride;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.IKaryotype;

/**
 * Karyotype-aware codecs for sparse chromosome-keyed maps, shared by every place that serializes a partial set of
 * genome data (species genome overrides, mutation result alleles, taxon defaults, ...).
 * <p>
 * Keys are chromosome IDs. Values are serialized inline via the chromosome's own value codec, either as a single
 * {@link Allele} or as an {@link AlleleOverride} that may name one side of the pair. Both the JSON/NBT codec and the
 * network stream codec are keyed against a specific {@link IKaryotype}, which is only available once the owning
 * species type has been registered - callers build these lazily.
 */
public final class GenomeCodecs {
	private GenomeCodecs() {
	}

	/**
	 * @return A codec for a sparse {@code chromosome id -> allele} map, keyed against the given karyotype
	 */
	public static Codec<Map<Identifier, Allele<?>>> alleleMapCodec(IKaryotype karyotype) {
		return mapCodec(karyotype, GenomeCodecs::alleleCodec);
	}

	/**
	 * @return A stream codec for a sparse {@code chromosome id -> allele} map, keyed against the given karyotype
	 */
	public static StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, Allele<?>>> alleleMapStreamCodec(IKaryotype karyotype) {
		return mapStreamCodec(karyotype, GenomeCodecs::alleleStreamCodec);
	}

	/**
	 * @return A codec for a sparse {@code chromosome id -> override} map, keyed against the given karyotype
	 */
	public static Codec<Map<Identifier, AlleleOverride<?>>> overrideMapCodec(IKaryotype karyotype) {
		return mapCodec(karyotype, GenomeCodecs::overrideCodec);
	}

	/**
	 * @return A stream codec for a sparse {@code chromosome id -> override} map, keyed against the given karyotype
	 */
	public static StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, AlleleOverride<?>>> overrideMapStreamCodec(IKaryotype karyotype) {
		return mapStreamCodec(karyotype, GenomeCodecs::overrideStreamCodec);
	}

	private static <T> Codec<Map<Identifier, T>> mapCodec(IKaryotype karyotype, Function<IChromosome<?>, Codec<T>> entryCodec) {
		return Codec.<IChromosome<?>, T>dispatchedMap(karyotype.chromosomeKeyCodec(), entryCodec::apply)
			.xmap(
				byChromosome -> {
					Map<Identifier, T> byId = new LinkedHashMap<>(byChromosome.size());
					byChromosome.forEach((chromosome, entry) -> byId.put(chromosome.id(), entry));
					return byId;
				},
				byId -> {
					Map<IChromosome<?>, T> byChromosome = new LinkedHashMap<>(byId.size());
					byId.forEach((id, entry) -> {
						// Unknown chromosome ids are dropped on encode; a parsed map only ever holds valid ids.
						IChromosome<?> chromosome = karyotype.getChromosome(id);
						if (chromosome != null) {
							byChromosome.put(chromosome, entry);
						}
					});
					return byChromosome;
				}
			);
	}

	private static <T> StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, T>> mapStreamCodec(IKaryotype karyotype, Function<IChromosome<?>, StreamCodec<RegistryFriendlyByteBuf, T>> entryStreamCodec) {
		return StreamCodec.of(
			(buf, map) -> {
				buf.writeVarInt(map.size());
				map.forEach((id, entry) -> {
					Identifier.STREAM_CODEC.encode(buf, id);
					entryStreamCodec.apply(chromosome(karyotype, id)).encode(buf, entry);
				});
			},
			buf -> {
				int size = buf.readVarInt();
				Map<Identifier, T> map = new LinkedHashMap<>(size);
				for (int i = 0; i < size; i++) {
					Identifier id = Identifier.STREAM_CODEC.decode(buf);
					map.put(id, entryStreamCodec.apply(chromosome(karyotype, id)).decode(buf));
				}
				return map;
			}
		);
	}

	private static IChromosome<?> chromosome(IKaryotype karyotype, Identifier id) {
		IChromosome<?> chromosome = karyotype.getChromosome(id);
		if (chromosome == null) {
			throw new IllegalStateException("Unknown chromosome in genome allele map: " + id);
		}
		return chromosome;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Codec<Allele<?>> alleleCodec(IChromosome<?> chromosome) {
		return (Codec) Allele.codec(chromosome.valueCodec());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static StreamCodec<RegistryFriendlyByteBuf, Allele<?>> alleleStreamCodec(IChromosome<?> chromosome) {
		return (StreamCodec) Allele.streamCodec(chromosome.valueStreamCodec());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Codec<AlleleOverride<?>> overrideCodec(IChromosome<?> chromosome) {
		return (Codec) AlleleOverride.codec(chromosome.valueCodec());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static StreamCodec<RegistryFriendlyByteBuf, AlleleOverride<?>> overrideStreamCodec(IChromosome<?> chromosome) {
		return (StreamCodec) AlleleOverride.streamCodec(chromosome.valueStreamCodec());
	}
}
