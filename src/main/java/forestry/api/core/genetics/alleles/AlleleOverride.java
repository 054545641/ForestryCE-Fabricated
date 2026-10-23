package forestry.api.core.genetics.alleles;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One chromosome's entry in a sparse set of genome overrides: an active allele, an inactive allele, or both.
 * <p>
 * This is the serialized counterpart of the three {@link forestry.api.plugin.IGenomeBuilder} setters. A side that is
 * {@code null} is left at whatever the karyotype and taxon defaults produced, so
 * {@link forestry.api.plugin.IGenomeBuilder#setActive} and {@link forestry.api.plugin.IGenomeBuilder#setInactive}
 * are expressible in data, not only in code.
 *
 * <p>The JSON form is a single allele when both sides hold the same one, so datapacks and addons written against the
 * older format keep loading:
 *
 * <pre>
 * "forestry:bee_effect": { "value": "forestry:bee_effect_mycophilic" }
 * "forestry:speed":      { "value": 1.4, "dominant": true }
 * </pre>
 *
 * To set the sides apart, name each one and give it a whole allele of its own:
 *
 * <pre>
 * "forestry:bee_effect": { "active": { "value": "forestry:bee_effect_mycophilic" } }
 * "forestry:speed":      {
 *     "active":   { "value": 1.7, "dominant": true },
 *     "inactive": { "value": 0.6 }
 * }
 * </pre>
 *
 * Ex. {@code { "inactive": { "value": 0.6 } }} -> leave the active allele alone, set the inactive one to 0.6
 *
 * @param active   The active (expressed) allele, or {@code null} to leave the default in place.
 * @param inactive The inactive allele, or {@code null} to leave the default in place.
 * @param <V>      The type of value held by this override's alleles.
 */
public record AlleleOverride<V>(@Nullable Allele<V> active, @Nullable Allele<V> inactive) {
	public AlleleOverride {
		if (active == null && inactive == null) {
			throw new IllegalArgumentException("A genome override must set the active allele, the inactive allele, or both");
		}
	}

	/**
	 * Creates an override that sets both the active and inactive allele, the equivalent of
	 * {@link forestry.api.plugin.IGenomeBuilder#set}.
	 */
	public static <V> AlleleOverride<V> both(Allele<V> allele) {
		return new AlleleOverride<>(allele, allele);
	}

	/**
	 * Creates an override that sets only the active allele, the equivalent of
	 * {@link forestry.api.plugin.IGenomeBuilder#setActive}.
	 */
	public static <V> AlleleOverride<V> onlyActive(Allele<V> allele) {
		return new AlleleOverride<>(allele, null);
	}

	/**
	 * Creates an override that sets only the inactive allele, the equivalent of
	 * {@link forestry.api.plugin.IGenomeBuilder#setInactive}.
	 */
	public static <V> AlleleOverride<V> onlyInactive(Allele<V> allele) {
		return new AlleleOverride<>(null, allele);
	}

	/**
	 * Layers another override on top of this one. Used when a genome closure calls more than one setter for the same
	 * chromosome, so the later call wins per side rather than for the whole pair.
	 *
	 * @param other The override applied over this one.
	 * @return A new override taking each side from {@code other} where it sets one, otherwise from this one
	 */
	public AlleleOverride<V> overrideWith(AlleleOverride<V> other) {
		return new AlleleOverride<>(
			other.active != null ? other.active : this.active,
			other.inactive != null ? other.inactive : this.inactive
		);
	}

	/**
	 * @return {@code true} if both sides are set to the same allele
	 */
	public boolean isSameAlleles() {
		return this.active != null && this.active.equals(this.inactive);
	}

	/**
	 * @return A codec for one override, serializing values inline via the given value codec
	 */
	public static <V> Codec<AlleleOverride<V>> codec(Codec<V> valueCodec) {
		Codec<Allele<V>> alleleCodec = Allele.codec(valueCodec);
		// A whole allele and a pair of named sides can never be confused: one always has "value", the other never does
		return Codec.either(alleleCodec, perSideCodec(alleleCodec)).xmap(
			either -> either.map(AlleleOverride::both, Function.identity()),
			override -> override.isSameAlleles() ? Either.left(override.active()) : Either.right(override)
		);
	}

	/**
	 * @return A stream codec for one override: a side mask byte, then each present side's allele
	 */
	public static <V> StreamCodec<RegistryFriendlyByteBuf, AlleleOverride<V>> streamCodec(StreamCodec<RegistryFriendlyByteBuf, V> valueStreamCodec) {
		StreamCodec<RegistryFriendlyByteBuf, Allele<V>> alleleStreamCodec = Allele.streamCodec(valueStreamCodec);
		return StreamCodec.of(
			(buf, override) -> {
				Allele<V> active = override.active();
				Allele<V> inactive = override.inactive();
				buf.writeByte((active != null ? ACTIVE_SIDE : 0) | (inactive != null ? INACTIVE_SIDE : 0));
				if (active != null) {
					alleleStreamCodec.encode(buf, active);
				}
				if (inactive != null) {
					alleleStreamCodec.encode(buf, inactive);
				}
			},
			buf -> {
				int sides = buf.readByte();
				Allele<V> active = (sides & ACTIVE_SIDE) != 0 ? alleleStreamCodec.decode(buf) : null;
				Allele<V> inactive = (sides & INACTIVE_SIDE) != 0 ? alleleStreamCodec.decode(buf) : null;
				return new AlleleOverride<>(active, inactive);
			}
		);
	}

	private static final int ACTIVE_SIDE = 1;
	private static final int INACTIVE_SIDE = 2;

	/**
	 * The two sides before validation, since an override with neither side set cannot be constructed.
	 */
	private record Sides<V>(Optional<Allele<V>> active, Optional<Allele<V>> inactive) {
	}

	private static <V> Codec<AlleleOverride<V>> perSideCodec(Codec<Allele<V>> alleleCodec) {
		return RecordCodecBuilder.<Sides<V>>create(instance -> instance.group(
			alleleCodec.optionalFieldOf("active").forGetter(Sides::active),
			alleleCodec.optionalFieldOf("inactive").forGetter(Sides::inactive)
		).apply(instance, Sides::new)).comapFlatMap(
			sides -> sides.active().isEmpty() && sides.inactive().isEmpty()
				? DataResult.error(() -> "A genome override must name \"active\", \"inactive\", or both")
				: DataResult.success(new AlleleOverride<>(sides.active().orElse(null), sides.inactive().orElse(null))),
			override -> new Sides<>(Optional.ofNullable(override.active()), Optional.ofNullable(override.inactive()))
		);
	}
}
