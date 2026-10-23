package forestry.arboriculture.client;

import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.arboriculture.ForestryLeafSprites;
import forestry.arboriculture.leaves.*;
import forestry.core.platform.models.ForestryItemTintSource;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class FabricLeafModels {
    private static final Identifier FALLBACK = ForestryConstants.forestry("block/leaves/oak");

    public static void initialize() {
        ModelLoadingPlugin.register(context -> {
            for (var block : BuiltInRegistries.BLOCK) if (isLeaves(block)) {
                context.registerBlockStateResolver(block, resolver -> {
                    var model = new Unbaked().asRoot();
                    block.getStateDefinition().getPossibleStates().forEach(state -> resolver.setModel(state, model));
                });
            }
        });
        ItemModels.ID_MAPPER.put(ForestryConstants.forestry("leaves"), ItemUnbaked.CODEC);
    }

    private static boolean isLeaves(Block block) {
        return block instanceof AbstractLeavesBlock || block instanceof DecorativeLeavesBlock;
    }

    private record Key(Identifier foliage, @Nullable Identifier fruit) {}

    private static Key itemKey(ItemStack stack, BlockState state, @Nullable ClientLevel level) {
        var data = forestry.core.platform.util.NBTUtilForestry.getItemStackTag(stack);
        if (!(state.getBlock() instanceof BlockForestryLeaves) || data == null) return key(state, null, null);
        var leaves = new LeavesBlockEntity(BlockPos.ZERO, state);
        if (level != null) leaves.setLevel(level);
        forestry.core.platform.util.ValueIOUtil.readTag(data, level == null ? net.minecraft.core.RegistryAccess.EMPTY : level.registryAccess(), leaves::loadCustomOnly);
        var tree = leaves.getTree();
        if (tree == null) return key(state, null, null);
        var sprite = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(tree.getSpecies());
        if (sprite == null) sprite = ForestryLeafSprites.OAK;
        forestry.api.arboriculture.genetics.IFruit fruit = tree.getGenome().resolveActive(forestry.api.core.genetics.alleles.TreeChromosomes.FRUIT);
        return new Key(sprite.get(leaves.isPollinated(), Minecraft.getInstance().options.cutoutLeaves().get()),
            leaves.hasFruit() ? fruit.getDecorativeSprite() : null);
    }

    private static Key key(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        var block = state.getBlock();
        ForestryLeafType leafType = block instanceof DefaultLeavesBlock leaves ? leaves.getType()
            : block instanceof DefaultFruitLeavesBlock leaves ? leaves.getType()
            : block instanceof DecorativeLeavesBlock leaves ? leaves.getType() : null;
        var snapshot = level != null && pos != null ? level.getBlockEntityRenderData(pos) : null;
        var data = snapshot instanceof LeavesBlockEntity.RenderData leaves ? leaves : null;
        var species = data != null ? data.species() : leafType == null ? null : leafType.getIndividual().getSpecies();
        var sprite = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(species);
        if (sprite == null) sprite = ForestryLeafSprites.OAK;
        Identifier fruit = data == null ? null : data.fruitTexture();
        if (leafType != null && !(block instanceof DefaultLeavesBlock)) fruit = leafType.getFruit().getDecorativeSprite();
        return new Key(sprite.get(data != null && data.pollinated(), Minecraft.getInstance().options.cutoutLeaves().get()), fruit);
    }

    private record Part(QuadCollection quads, Material.Baked particleMaterial) implements BlockStateModelPart {
        @Override public List<BakedQuad> getQuads(@Nullable Direction side) { return this.quads.getQuads(side); }
        @Override public boolean useAmbientOcclusion() { return true; }
        @Override public int materialFlags() { return this.quads.materialFlags(); }
    }

    private static Map<Identifier, Part> bakeParts(ModelBaker baker) {
        var parts = new HashMap<Identifier, Part>();
        Minecraft.getInstance().getResourceManager().listResources("textures/block/leaves", id -> id.getPath().endsWith(".png")).keySet().forEach(file -> {
            var texture = file.withPath(file.getPath().substring(9, file.getPath().length() - 4));
            var material = baker.materials().get(new Material(texture, true), () -> "Forestry leaves");
            int tint = texture.getPath().contains("fruits.") ? AbstractLeavesBlock.FRUIT_COLOR_INDEX : AbstractLeavesBlock.FOLIAGE_COLOR_INDEX;
            var info = BakedQuad.MaterialInfo.of(material, material.sprite().transparency(), tint, true, 0);
            var quads = new QuadCollection.Builder();
            for (var side : Direction.values()) {
                quads.addCulledFace(side, FaceBakery.bakeQuad(baker.interner(), new Vector3f(0, 0, 0), new Vector3f(16, 16, 16),
                    new CuboidFace.UVs(0, 0, 16, 16), Quadrant.R0, info, side, BlockModelRotation.IDENTITY, null));
            }
            parts.put(texture, new Part(quads.build(), material));
        });
        return Map.copyOf(parts);
    }

    private static List<Part> selected(Map<Identifier, Part> parts, Key key) {
        var foliage = Objects.requireNonNull(parts.getOrDefault(key.foliage, parts.get(FALLBACK)), "Missing Forestry oak leaves");
        var fruit = key.fruit == null ? null : parts.get(key.fruit);
        return fruit == null ? List.of(foliage) : List.of(foliage, fruit);
    }

    public record Unbaked() implements BlockStateModel.Unbaked {
        @Override public void resolveDependencies(Resolver resolver) {}
        @Override public BlockStateModel bake(ModelBaker baker) {
            var parts = bakeParts(baker);
            var fallback = Objects.requireNonNull(parts.get(FALLBACK));
            return new BlockStateModel() {
                @Override public void collectParts(RandomSource random, List<BlockStateModelPart> destination) { destination.add(fallback); }
                @Override public Material.Baked particleMaterial() { return fallback.particleMaterial; }
                @Override public int materialFlags() { return fallback.materialFlags(); }
                @Override public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) {
                    for (var part : selected(parts, key(state, level, pos))) for (var side : Direction.values()) {
                        if (!cull.test(side)) for (var quad : part.getQuads(side)) emitter.fromBakedQuad(quad).cullFace(side).emit();
                    }
                }
                @Override public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) { return selected(parts, key(state, level, pos)).getFirst().particleMaterial; }
                @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
                    return selected(parts, key(state, level, pos)).stream().mapToInt(Part::materialFlags).reduce(0, (first, second) -> first | second);
                }
                @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return key(state, level, pos); }
            };
        }
    }

    public record ItemUnbaked() implements ItemModel.Unbaked {
        public static final MapCodec<ItemUnbaked> CODEC = MapCodec.unit(new ItemUnbaked());
        @Override public MapCodec<ItemUnbaked> type() { return CODEC; }
        @Override public void resolveDependencies(Resolver resolver) { resolver.markDependency(Identifier.withDefaultNamespace("block/block")); }
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            var baker = context.blockModelBaker();
            var parts = bakeParts(baker);
            var transforms = baker.getModel(Identifier.withDefaultNamespace("block/block")).getTopTransforms();
            var models = new ConcurrentHashMap<Key, ItemModel>();
            return (renderState, stack, resolver, display, level, owner, seed) -> {
                var state = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState() : forestry.arboriculture.features.ArboricultureBlocks.LEAVES.defaultState();
                var key = itemKey(stack, state, level);
                var model = models.computeIfAbsent(key, selectedKey -> {
                    var selected = selected(parts, selectedKey);
                    var quads = new QuadCollection.Builder();
                    selected.forEach(part -> part.quads.getAll().forEach(quads::addUnculledFace));
                    return new CuboidItemModelWrapper(List.of(new ForestryItemTintSource(0), new ForestryItemTintSource(1), new ForestryItemTintSource(2)),
                        quads.build(), new ModelRenderProperties(true, selected.getFirst().particleMaterial, transforms), transform);
                });
                model.update(renderState, stack, resolver, display, level, owner, seed);
            };
        }
    }
}
