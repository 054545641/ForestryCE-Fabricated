package forestry.core.platform.util;

import net.minecraft.network.codec.StreamCodec;

import java.util.function.Supplier;

/**
 * A {@link StreamCodec} whose delegate is resolved on first use. The stream counterpart of
 * {@link LazyMapCodec}.
 */
public class LazyStreamCodec<B, A> implements StreamCodec<B, A> {
	private final Supplier<StreamCodec<B, A>> supplier;
	private volatile StreamCodec<B, A> delegate;

	public LazyStreamCodec(Supplier<StreamCodec<B, A>> supplier) {
		this.supplier = supplier;
	}

	private StreamCodec<B, A> delegate() {
		StreamCodec<B, A> d = this.delegate;
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
	public A decode(B buffer) {
		return delegate().decode(buffer);
	}

	@Override
	public void encode(B buffer, A value) {
		delegate().encode(buffer, value);
	}
}
