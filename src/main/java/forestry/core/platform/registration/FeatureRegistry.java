package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.*;
import java.util.function.*;

/** The owner mod and the persisted registry namespace are separate: all existing IDs remain forestry:*. */
public final class FeatureRegistry implements IFeatureRegistry {
    private final Identifier moduleId;
    private final String ownerModId;
    private final List<IModFeature> features = new ArrayList<>();
    private final Map<ResourceKey<?>, RegistrationBatch<?>> registries = new LinkedHashMap<>();
    private final List<Runnable> listeners = new ArrayList<>();

    FeatureRegistry(Identifier moduleId, String ownerModId) {
        this.moduleId = moduleId;
        this.ownerModId = ownerModId;
    }

    @Override @SuppressWarnings("unchecked")
    public <V> RegistrationBatch<V> getRegistry(ResourceKey<? extends Registry<V>> key) {
        return (RegistrationBatch<V>) this.registries.computeIfAbsent(key, ignored -> RegistrationBatch.create(key, this.moduleId.getNamespace()));
    }

    @Override public <I extends Item> FeatureItem<I> item(Function<Item.Properties, I> factory, String name) {
        return item(factory, Item.Properties::new, name);
    }

    @Override public <I extends Item> FeatureItem<I> item(Function<Item.Properties, I> factory, Supplier<Item.Properties> properties, String name) {
        Identifier id = this.moduleId.withPath(name);
        FeatureItem<I> feature = new FeatureItem<>(this, this.moduleId, name,
            () -> factory.apply(properties.get().setId(ResourceKey.create(Registries.ITEM, id))));
        this.features.add(feature);
        return feature;
    }

    @Override public <B extends Block, I extends BlockItem> FeatureBlock<B, I> block(Function<BlockBehaviour.Properties, B> factory,
            Supplier<BlockBehaviour.Properties> properties, BiFunction<B, Item.Properties, I> itemFactory, String name) {
        Identifier id = this.moduleId.withPath(name);
        FeatureBlock<B, I> feature = new FeatureBlock<>(this, this.moduleId, name,
            () -> factory.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, id))),
            itemFactory == null ? null : block -> itemFactory.apply(block,
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix()));
        this.features.add(feature);
        return feature;
    }

    @Override public FeatureCreativeTab creativeTab(String name, Consumer<CreativeModeTab.Builder> builder) {
        FeatureCreativeTab feature = new FeatureCreativeTab(this, this.moduleId, name, builder);
        this.features.add(feature);
        return feature;
    }

    @Override public Collection<IModFeature> getFeatures() { return Collections.unmodifiableList(this.features); }
    public void addFeature(IModFeature feature) { this.features.add(feature); }
    @Override public <B extends Block, S extends forestry.api.core.IBlockSubtype> FeatureBlockGroup.Builder<B, S> blockGroup(BiFunction<BlockBehaviour.Properties, S, B> factory, Collection<S> variants) {
        return new FeatureBlockGroup.Builder<>(this, variants, factory);
    }
    @Override public <B extends Block, R extends forestry.api.core.IBlockSubtype, C extends forestry.api.core.IBlockSubtype> FeatureBlockTable.Builder<B, R, C> blockTable(forestry.core.platform.util.datastructures.TriFunction<BlockBehaviour.Properties, R, C, B> factory, R[] rows, C[] columns) {
        var builder = new FeatureBlockTable.Builder<B, R, C>(this, factory);
        builder.rowTypes(rows).columnTypes(columns);
        return builder;
    }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> FeatureTileType<T> tile(net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier<T> factory, String name, Supplier<Collection<? extends Block>> blocks) {
        var feature = new FeatureTileType<>(this, moduleId, name, factory, blocks); addFeature(feature); return feature;
    }
    @Override public void addRegistryListener(ResourceKey<? extends Registry<?>> key, Runnable action) {
        // Factories may resolve dependencies early. Run listeners only after all declared registries are bound.
        listeners.add(action);
    }
    void notifyBound() { listeners.forEach(Runnable::run); listeners.clear(); }
    @Override public <T extends net.minecraft.world.entity.Entity> FeatureEntityType<T> entity(net.minecraft.world.entity.EntityType.EntityFactory<T> factory, net.minecraft.world.entity.MobCategory category, String name,
            UnaryOperator<net.minecraft.world.entity.EntityType.Builder<T>> properties, Supplier<net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder> attributes) {
        var feature = new FeatureEntityType<>(this, moduleId, name, properties, factory, category, attributes); addFeature(feature); return feature;
    }
    @Override public <M extends net.minecraft.world.inventory.AbstractContainerMenu> FeatureMenuType<M> menuType(FeatureMenuType.MenuFactory<M> factory, String name) {
        var feature = new FeatureMenuType<>(this, moduleId, name, factory); addFeature(feature); return feature;
    }
    @Override public <I extends Item, S extends forestry.api.core.IItemSubtype> FeatureItemGroup<I, S> itemGroup(BiFunction<S, Item.Properties, I> factory, String prefix, S[] variants) {
        return new FeatureItemGroup.Builder<I, S>(this, factory, ignored -> new Item.Properties()).types(variants).identifierPrefix(prefix).create();
    }
    @Override public <I extends Item, S extends forestry.api.core.IItemSubtype> FeatureItemGroup.Builder<I, S> itemGroup(BiFunction<S, Item.Properties, I> factory, S[] variants) {
        var builder = new FeatureItemGroup.Builder<I, S>(this, factory, ignored -> new Item.Properties());
        builder.types(variants);
        return builder;
    }
    @Override public <I extends Item, R extends forestry.api.core.IItemSubtype, C extends forestry.api.core.IItemSubtype> FeatureItemTable<I, R, C> itemTable(forestry.core.platform.util.datastructures.TriFunction<R, C, Item.Properties, I> factory, R[] rows, C[] columns, String prefix) {
        return new FeatureItemTable.Builder<I, R, C>(this, factory).rowTypes(rows).columnTypes(columns).identifier(prefix).create();
    }
    @Override public <R extends net.minecraft.world.item.crafting.Recipe<?>> FeatureRecipeType<R> recipeType(String name, Supplier<net.minecraft.world.item.crafting.RecipeSerializer<? extends R>> serializer) {
        var feature = new FeatureRecipeType<>(this, this.moduleId, name, serializer);
        addFeature(feature);
        return feature;
    }
    @Override public FeatureFluid.Builder fluid(String name) { return new FeatureFluid.Builder(this, this.moduleId, name); }
    @Override public Identifier getModuleId() { return this.moduleId; }
    public String getOwnerModId() { return this.ownerModId; }
}
