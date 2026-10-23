package forestry.core.platform.models;

import forestry.core.platform.models.baker.ModelBaker;
import forestry.core.platform.models.baker.ModelBakerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;
import java.util.List;

/** Dynamic block geometry and item geometry share baking, but have separate native entry points. */
public abstract class ModelBlockDefault<B extends Block, K> implements ItemModel {
    protected final Class<B> blockClass;
    private java.util.function.Function<net.minecraft.resources.Identifier, net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites = forestry.core.platform.util.ResourceUtil::getBlockSprite;
    public final void setSpriteGetter(java.util.function.Function<net.minecraft.resources.Identifier, net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites) { this.sprites = sprites; }
    protected final net.minecraft.client.renderer.texture.TextureAtlasSprite getSprite(net.minecraft.resources.Identifier id) { return this.sprites.apply(id); }
    protected ModelBlockDefault(Class<B> blockClass) { this.blockClass = blockClass; }

    protected ModelBakerModel bakeModel(BlockState state, K key, B block, ModelData data) {
        ModelBaker baker = new ModelBaker();
        bakeBlock(block, data, key, baker, false);
        ModelBakerModel model = baker.bake(false);
        onCreateModel(model);
        return model;
    }
    protected ModelBakerModel getModel(BlockState state, ModelData data) {
        return bakeModel(state, getWorldKey(state, data), this.blockClass.cast(state.getBlock()), data);
    }
    protected ModelBakerModel bakeModel(ItemStack stack, @Nullable Level level, K key) {
        ModelBaker baker = new ModelBaker();
        bakeBlock(this.blockClass.cast(Block.byItem(stack.getItem())), ModelData.EMPTY, key, baker, true);
        return baker.bake(true);
    }
    protected ModelBakerModel getModel(ItemStack stack, @Nullable Level level) { return bakeModel(stack, level, getInventoryKey(stack)); }
    protected void onCreateModel(ModelBakerModel model) { model.setAmbientOcclusion(true); }

    public final BlockStateModel forState(BlockState boundState) {
        return new BlockStateModel() {
            private ModelBakerModel model(ModelData data) { return getModel(boundState, data); }
            @Override public void collectParts(RandomSource random, List<BlockStateModelPart> parts) { parts.add(model(ModelData.EMPTY)); }
            @Override public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
                parts.add(model(level.getModelData(pos)));
            }
            @Override public Material.Baked particleMaterial() { return model(ModelData.EMPTY).particleMaterial(); }
            @Override public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
                return model(level.getModelData(pos)).particleMaterial();
            }
            @Override public int materialFlags() { return model(ModelData.EMPTY).materialFlags(); }
            @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) { return model(level.getModelData(pos)).materialFlags(); }
            @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
                return model(level.getModelData(pos));
            }
        };
    }
    @Override public void update(ItemStackRenderState output, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context,
                                  @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        getModel(stack, level).asItemModel().update(output, stack, resolver, context, level, owner, seed);
    }
    protected abstract K getInventoryKey(ItemStack stack);
    protected abstract K getWorldKey(BlockState state, ModelData data);
    protected abstract void bakeBlock(B block, ModelData data, K key, ModelBaker baker, boolean inventory);
}
