package forestry.core.platform.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.KeyCompressor;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link MapCodec} whose delegate is resolved on first use. Needed where a codec depends on
 * content (e.g. a species type's karyotype) that does not exist yet when the owning
 * {@code RecipeSerializer} record is constructed during registry events.
 */
public class LazyMapCodec<A> extends MapCodec<A> {
	private final Supplier<MapCodec<A>> supplier;
	private volatile MapCodec<A> delegate;

	public LazyMapCodec(Supplier<MapCodec<A>> supplier) {
		this.supplier = supplier;
	}

	private MapCodec<A> delegate() {
		MapCodec<A> d = this.delegate;
		if (d == null) {
			synchronized (this) {
				d = this.delegate;
				if (d == null) {
					d = this.supplier.get();
					this.delegate = d;
				}
			}
		}
		return d;
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return delegate().keys(ops);
	}

	@Override
	public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
		return delegate().decode(ops, input);
	}

	@Override
	public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		return delegate().encode(input, ops, prefix);
	}

	@Override
	public <T> KeyCompressor<T> compressor(DynamicOps<T> ops) {
		return delegate().compressor(ops);
	}
}
