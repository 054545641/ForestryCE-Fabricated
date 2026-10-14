package forestry.environment;

import com.mojang.logging.LogUtils;
import forestry.core.content.backpacks.client.FilledCrateModel;
import forestry.core.platform.models.*;
import forestry.core.platform.models.baker.ModelBaker;
import forestry.core.platform.models.baker.ModelBakerModel;
import forestry.core.platform.util.ResourceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EnvironmentProbe.ID, value = Dist.CLIENT)
public final class N5ModelProbe {
    private static final ModelProperty<Identifier> TEXTURE = new ModelProperty<>();
    private static final ProbeModel MODEL = new ProbeModel();
    private static int passes;
    @SubscribeEvent public static void loaders(ModelEvent.RegisterLoaders event) {
        event.register(Identifier.parse("forestry:filled_crate"), new FilledCrateModel.Loader());
    }
    @SubscribeEvent public static void itemModels(RegisterItemModelsEvent event) {
        event.register(Identifier.parse("forestry:fluid_container"), FluidContainerModel.Unbaked.MAP_CODEC);
        event.register(Identifier.parse("forestry:filled_crate"), FilledCrateModel.Item.CODEC);
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Identifier.parse("forestry:item"), ForestryItemTintSource.CODEC);
    }
    @SubscribeEvent public static void reload(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.parse("forestry_environment:model_cache"), (state, tasks, barrier, executor) ->
            barrier.wait(net.minecraft.util.Unit.INSTANCE).thenRunAsync(ModelBlockCached::clear, executor));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    public static void verify(Minecraft client) {
        var stone = ResourceUtil.getBlockSprite("minecraft:block/stone");
        var leaves = ResourceUtil.getBlockSprite("minecraft:block/oak_leaves");
        var baker = new ModelBaker();
        baker.addBlockModel(stone, 0).addBlockModel(leaves, 1);
        baker.setParticleSprite(stone);
        var model = baker.bake(false);
        check(model.getQuads(null).isEmpty(), "Culled faces must not also appear in the unculled list");
        check(model.quads().getAll().size() == 12, "Two layers produce exactly twelve faces");
        for (Direction face : Direction.values()) {
            check(model.getQuads(face).size() == 2, "Exactly two quads per culling direction");
            check(model.getQuads(face).stream().allMatch(q -> q.direction() == face), "Culling follows the quad normal");
        }
        check(model.particleMaterial().sprite() == stone, "Particle material belongs to this bake");
        var flipped = baker.bake(true);
        check(flipped.getQuads(Direction.NORTH).getFirst().direction() == Direction.NORTH, "Flipped culling uses transformed directions");
        check(model.quads().getAll().size() == 12, "Repeated baking does not accumulate faces");

        BlockState state = Blocks.OAK_LEAVES.defaultBlockState();
        Identifier spruce = Identifier.parse("minecraft:block/spruce_leaves");
        ModelData data = ModelData.builder().with(TEXTURE, spruce).build();
        var first = MODEL.world(state, data);
        check(first == MODEL.world(state, data), "Identical data reuses the cached geometry");
        check(first != MODEL.world(Blocks.BIRCH_LEAVES.defaultBlockState(), data), "Block states do not alias cache entries");
        var world = (BlockAndTintGetter) java.lang.reflect.Proxy.newProxyInstance(N5ModelProbe.class.getClassLoader(), new Class<?>[] {BlockAndTintGetter.class},
            (proxy, method, args) -> { if (method.getName().equals("getModelData")) return data; throw new UnsupportedOperationException(method.getName()); });
        var bound = MODEL.forState(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        bound.collectParts(world, BlockPos.ZERO, state, RandomSource.create(), parts);
        check(parts.size() == 1 && parts.getFirst() == first, "Native block collection reads supplied ModelData");
        check(bound.particleMaterial(world, BlockPos.ZERO, state).sprite() == ResourceUtil.getBlockSprite(spruce), "Particles follow current ModelData and atlas generation");
        check(MODEL.world(state, ModelData.EMPTY).particleMaterial().sprite() == stone, "Missing ModelData uses the default");
        ModelBlockCached.clear();
        check(first != MODEL.world(state, data), "Resource reload invalidates cached geometry");

        var output = new ItemStackRenderState();
        model.asItemModel().update(output, ItemStack.EMPTY, client.getItemModelResolver(), ItemDisplayContext.GUI, null, null, 0);
        check(!output.isEmpty(), "Baked cube produces native item layers");
        for (String id : List.of("forestry:crated_apatite", "forestry:crated_grass_block", "forestry:can", "forestry:wax_capsule", "forestry:refractory_capsule", "forestry_environment:water_can", "forestry_environment:lava_can")) {
            var itemModel = client.getModelManager().getItemModel(Identifier.parse(id));
            check(!(itemModel instanceof net.minecraft.client.renderer.item.MissingItemModel), "Production model baked: " + id);
            {
                output.clear();
                itemModel.update(output, ItemStack.EMPTY, client.getItemModelResolver(), ItemDisplayContext.GUI, null, null, 0);
                check(!output.isEmpty(), "Item model emits geometry: " + id);
                var particle = output.pickParticleMaterial(RandomSource.create());
                check(particle != null && !particle.sprite().contents().name().getPath().equals("missingno"), "Model has real particle material: " + id);
            }
        }
        LogUtils.getLogger().info("N5 production model probe passed (pass {}): face counts/culling, native block and item output, ModelData particles, cache invalidation, crate geometry, empty containers and explicit water/lava model variants (no world-bound ItemStacks)", ++passes);
    }

    private static final class ProbeModel extends ModelBlockCached<Block, Identifier> {
        private ProbeModel() { super(Block.class); }
        ModelBakerModel world(BlockState state, ModelData data) { return getModel(state, data); }
        @Override protected Identifier getInventoryKey(ItemStack stack) { return Identifier.parse("minecraft:block/stone"); }
        @Override protected Identifier getWorldKey(BlockState state, ModelData data) {
            Identifier texture = data.get(TEXTURE);
            return texture == null ? Identifier.parse("minecraft:block/stone") : texture;
        }
        @Override protected void bakeBlock(Block block, ModelData data, Identifier key, ModelBaker baker, boolean inventory) {
            var sprite = getSprite(key);
            baker.addBlockModel(sprite, 0); baker.setParticleSprite(sprite);
        }
    }
}
