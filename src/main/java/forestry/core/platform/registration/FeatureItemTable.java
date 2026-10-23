package forestry.core.platform.registration;

import forestry.api.core.IItemSubtype;
import net.minecraft.world.item.Item;

import java.util.Collection;
import forestry.core.platform.util.datastructures.TriFunction;
import java.util.stream.Collectors;

public class FeatureItemTable<I extends Item, R extends IItemSubtype, C extends IItemSubtype> extends FeatureTable<FeatureItemTable.Builder<I, R, C>, FeatureItem<I>, R, C> {
	public FeatureItemTable(Builder<I, R, C> builder) {
		super(builder);
	}

	@Override
	protected FeatureItem<I> createFeature(Builder<I, R, C> builder, R rowType, C columnType) {
		return builder.registry.item(properties -> builder.constructor.apply(rowType, columnType, properties), builder.getIdentifier(rowType, columnType));
	}

	public Collection<I> getBlocks() {
		return this.featureByTypes.values().stream().map(IItemFeature::item).collect(Collectors.toList());
	}

	public static class Builder<I extends Item, R extends IItemSubtype, C extends IItemSubtype> extends FeatureTable.Builder<R, C, FeatureItemTable<I, R, C>> {
		private final FeatureRegistry registry;
		private final TriFunction<R, C, Item.Properties, I> constructor;

		public Builder(FeatureRegistry registry, TriFunction<R, C, Item.Properties, I> constructor) {
			super(registry);
			this.registry = registry;
			this.constructor = constructor;
		}

		public FeatureItemTable<I, R, C> create() {
			return new FeatureItemTable<>(this);
		}
	}
}
