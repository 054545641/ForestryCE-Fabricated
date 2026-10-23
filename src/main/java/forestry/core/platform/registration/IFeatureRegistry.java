package forestry.core.platform.registration;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.Collection;
import java.util.function.*;

/** Fabric F1 registration boundary. More content factories are migrated with their F2/F3 implementations. */
public interface IFeatureRegistry {
    <V> RegistrationBatch<V> getRegistry(ResourceKey<? extends Registry<V>> key);
    <I extends Item> FeatureItem<I> item(Function<Item.Properties, I> factory, String name);
    <I extends Item> FeatureItem<I> item(Function<Item.Properties, I> factory, Supplier<Item.Properties> properties, String name);
    <B extends Block, I extends BlockItem> FeatureBlock<B, I> block(Function<BlockBehaviour.Properties, B> factory,
        Supplier<BlockBehaviour.Properties> properties, BiFunction<B, Item.Properties, I> itemFactory, String name);
    FeatureCreativeTab creativeTab(String name, Consumer<CreativeModeTab.Builder> builder);
    default <B extends Block> FeatureBlock<B, BlockItem> block(Function<BlockBehaviour.Properties, B> factory, String name) { return block(factory, BlockBehaviour.Properties::of, null, name); }
    default <B extends Block, I extends BlockItem> FeatureBlock<B, I> block(Function<BlockBehaviour.Properties, B> factory, BiFunction<B, Item.Properties, I> itemFactory, String name) { return block(factory, BlockBehaviour.Properties::of, itemFactory, name); }
    <B extends Block, S extends forestry.api.core.IBlockSubtype> FeatureBlockGroup.Builder<B, S> blockGroup(BiFunction<BlockBehaviour.Properties, S, B> factory, Collection<S> variants);
    <B extends Block, R extends forestry.api.core.IBlockSubtype, C extends forestry.api.core.IBlockSubtype> FeatureBlockTable.Builder<B, R, C> blockTable(forestry.core.platform.util.datastructures.TriFunction<BlockBehaviour.Properties, R, C, B> factory, R[] rows, C[] columns);
    default <B extends Block, S extends forestry.api.core.IBlockSubtype> FeatureBlockGroup.Builder<B, S> blockGroup(BiFunction<BlockBehaviour.Properties, S, B> factory, S[] variants) { return blockGroup(factory, java.util.List.of(variants)); }
    <T extends net.minecraft.world.level.block.entity.BlockEntity> FeatureTileType<T> tile(net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier<T> factory, String name, Supplier<Collection<? extends Block>> blocks);
    void addRegistryListener(ResourceKey<? extends Registry<?>> key, Runnable action);
    <T extends net.minecraft.world.entity.Entity> FeatureEntityType<T> entity(net.minecraft.world.entity.EntityType.EntityFactory<T> factory, net.minecraft.world.entity.MobCategory category, String name,
        UnaryOperator<net.minecraft.world.entity.EntityType.Builder<T>> properties, Supplier<net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder> attributes);
    FeatureFluid.Builder fluid(String name);
    <M extends net.minecraft.world.inventory.AbstractContainerMenu> FeatureMenuType<M> menuType(FeatureMenuType.MenuFactory<M> factory, String name);
    <I extends Item, S extends forestry.api.core.IItemSubtype> FeatureItemGroup<I, S> itemGroup(BiFunction<S, Item.Properties, I> factory, String prefix, S[] variants);
    <I extends Item, S extends forestry.api.core.IItemSubtype> FeatureItemGroup.Builder<I, S> itemGroup(BiFunction<S, Item.Properties, I> factory, S[] variants);
    <I extends Item, R extends forestry.api.core.IItemSubtype, C extends forestry.api.core.IItemSubtype> FeatureItemTable<I, R, C> itemTable(forestry.core.platform.util.datastructures.TriFunction<R, C, Item.Properties, I> factory, R[] rows, C[] columns, String prefix);
    <R extends net.minecraft.world.item.crafting.Recipe<?>> FeatureRecipeType<R> recipeType(String name, Supplier<net.minecraft.world.item.crafting.RecipeSerializer<? extends R>> serializer);
    Collection<IModFeature> getFeatures();
    Identifier getModuleId();
}
