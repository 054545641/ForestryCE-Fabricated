package forestry.core.platform.registration;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** A declaration owned by Forestry, bound once through the vanilla registry API during initialization. */
public final class RegistryEntry<R, T extends R> implements Supplier<T> {
    private final ResourceKey<R> key;
    private final Supplier<Registry<R>> registry;
    private Supplier<T> factory;
    private T value;
    private Holder.Reference<R> holder;
    private boolean constructing;

    RegistryEntry(ResourceKey<R> key, Supplier<Registry<R>> registry, Supplier<T> factory) {
        this.key = key;
        this.registry = registry;
        this.factory = factory;
    }

    @Override
    public T get() {
        if (this.holder != null) return this.value;
        if (this.constructing) throw new IllegalStateException("Circular Forestry registration: " + this.key);
        this.constructing = true;
        try {
            Registry<R> target = this.registry.get();
            if (target.containsKey(this.key)) throw new IllegalStateException("Duplicate Forestry registration: " + this.key);
            this.value = Objects.requireNonNull(this.factory.get(), () -> "Null registration: " + this.key);
            Registry.register(target, this.key, this.value);
            this.holder = target.getOrThrow(this.key);
            this.factory = null;
            return this.value;
        } finally {
            this.constructing = false;
        }
    }

    public Identifier getId() { return this.key.identifier(); }
    public ResourceKey<R> getKey() { return this.key; }
    public Optional<T> asOptional() { return this.holder == null ? Optional.empty() : Optional.of(this.value); }
    public Holder<R> asHolder() { get(); return this.holder; }
    public boolean isBound() { return this.holder != null; }
}
