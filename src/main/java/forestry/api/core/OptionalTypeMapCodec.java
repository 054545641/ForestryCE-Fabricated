package forestry.api.core;

import com.mojang.serialization.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A dispatch map codec with an optional type key, falling back to a default type when the type key is absent.
 * Used in Forestry by Product and FluidProduct to keep most recipes short while allowing custom product types.
 *
 * <p>Vanilla writes the same "terse default, typed otherwise" idea as {@link Codec#withAlternative} (ex.
 * {@code LootItemCondition#DIRECT_CODEC}) or {@link Codec#either} plus an {@code xmap} that picks the side (ex.
 * {@code NumberProviders#CODEC}). Both pick the fallback by trial decoding, so an unknown type reports the
 * fallback's error instead of its own, and a bad entry whose fields happen to match the fallback decodes to the
 * wrong type. This dispatches on whether the key is present, which keeps those cases exact.
 *
 * @param <V> The dispatched value type. Ex. {@link IProduct}
 * @param <T> The serializer type registered to the registry. Ex. {@link ProductType}
 */
final class OptionalTypeMapCodec<V, T> extends MapCodec<V> {
	private final Registry<T> registry;
	private final String typeName;
	private final String typeKey;
	private final Supplier<T> defaultType;
	private final Function<V, T> typeGetter;
	private final Function<T, MapCodec<? extends V>> codecGetter;

	/**
	 * Creates a dispatch map codec with an optional type key.
	 *
	 * @param registry    The registry the type key is resolved against
	 * @param typeName    The noun used in error messages. Ex. "product type"
	 * @param typeKey     The name of the dispatch key. Ex. "type"
	 * @param defaultType The type used when the type key is absent. Uses a supplier to avoid static initialization
	 *                    ordering issues (ex. between IProduct.MAP_CODEC and Product.TYPE)
	 * @param typeGetter  The accessor for a value's type
	 * @param codecGetter The accessor for a type's map codec
	 * @return A map codec that dispatches on the key when present and falls back to the default type when absent
	 */
	static <V, T> MapCodec<V> of(Registry<T> registry, String typeName, String typeKey, Supplier<T> defaultType, Function<V, T> typeGetter, Function<T, MapCodec<? extends V>> codecGetter) {
		return new OptionalTypeMapCodec<>(registry, typeName, typeKey, defaultType, typeGetter, codecGetter);
	}

	private OptionalTypeMapCodec(Registry<T> registry, String typeName, String typeKey, Supplier<T> defaultType, Function<V, T> typeGetter, Function<T, MapCodec<? extends V>> codecGetter) {
		this.registry = registry;
		this.typeName = typeName;
		this.typeKey = typeKey;
		this.defaultType = defaultType;
		this.typeGetter = typeGetter;
		this.codecGetter = codecGetter;
	}

	@Override
	public <O> DataResult<V> decode(DynamicOps<O> ops, MapLike<O> input) {
		O typeValue = input.get(this.typeKey);
		DataResult<T> type = typeValue == null
			? DataResult.success(this.defaultType.get())
			: Identifier.CODEC.parse(ops, typeValue).flatMap(this::byId);
		return type.flatMap(t -> this.codecGetter.apply(t).decode(ops, input).map(value -> (V) value));
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <O> RecordBuilder<O> encode(V input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
		T type = this.typeGetter.apply(input);
		if (type != this.defaultType.get()) {
			Identifier id = this.registry.getKey(type);
			if (id == null) {
				return prefix.withErrorsFrom(DataResult.error(() -> "Unregistered " + this.typeName + ": " + type));
			}
			prefix.add(this.typeKey, Identifier.CODEC.encodeStart(ops, id));
		}
		return ((MapCodec) this.codecGetter.apply(type)).encode(input, ops, prefix);
	}

	@Override
	public <O> Stream<O> keys(DynamicOps<O> ops) {
		return Stream.of(ops.createString(this.typeKey));
	}

	private DataResult<T> byId(Identifier id) {
		T type = this.registry.getValue(id);
		if (type == null) {
			return DataResult.error(() -> "Unknown " + this.typeName + ": " + id);
		}
		return DataResult.success(type);
	}
}
