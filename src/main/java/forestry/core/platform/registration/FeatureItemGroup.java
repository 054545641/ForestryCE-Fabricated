package forestry.core.platform.registration;

import forestry.api.core.IItemSubtype;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.BiFunction;

public class FeatureItemGroup<I extends Item, S extends IItemSubtype> extends FeatureGroup<FeatureItemGroup.Builder<I, S>, FeatureItem<I>, S> {
	public FeatureItemGroup(Builder<I, S> builder) {
		super(builder);
	}

	public ArrayList<I> getItems() {
		ArrayList<I> items = new ArrayList<>(this.featureByType.size());
		for (FeatureItem<I> value : this.featureByType.values()) {
			items.add(value.item());
		}
		return items;
	}

	public Item[] itemArray() {
		return getItems().toArray(new Item[0]);
	}

	@Override
	protected FeatureItem<I> createFeature(Builder<I, S> builder, S type) {
		return builder.registry.item(properties -> builder.constructor.apply(type, properties), () -> builder.properties.apply(type), builder.getIdentifier(type));
	}

	public I item(S variant) {
		return get(variant).item();
	}

	public static class Builder<I extends Item, S extends IItemSubtype> extends FeatureGroup.Builder<S, FeatureItemGroup<I, S>> {
		private final FeatureRegistry registry;
		private final BiFunction<S, Item.Properties, I> constructor;
		private final Function<S, Item.Properties> properties;

		public Builder(FeatureRegistry registry, BiFunction<S, Item.Properties, I> constructor, Function<S, Item.Properties> properties) {
			super(registry, java.util.List.of());
			this.registry = registry;
			this.constructor = constructor;
			this.properties = properties;
		}

		@Override
		public FeatureItemGroup<I, S> create() {
			return new FeatureItemGroup<>(this);
		}
	}
}
