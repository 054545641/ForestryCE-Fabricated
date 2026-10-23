package forestry.arboriculture.models;

import com.mojang.serialization.MapCodec;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.arboriculture.sapling.SaplingBlockEntity;
import forestry.core.platform.models.ForestryItemTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import org.joml.Matrix4fc;
import java.util.*;

public final class SaplingModel {
    private static Set<Identifier> locations(boolean item) {
        var manager = IForestryClientApi.INSTANCE.getTreeManager();
        Set<Identifier> result = new HashSet<>();
        for (var pair : manager.getAllSaplingModels()) result.add(item ? pair.getSecond() : pair.getFirst());
        var fallback = manager.getDefaultSaplingModels();
        result.add(item ? fallback.getSecond() : fallback.getFirst());
        return result;
    }
    public record Block() implements CustomUnbakedBlockStateModel {
        public static final MapCodec<Block> CODEC = MapCodec.unit(new Block());
        @Override public MapCodec<Block> codec() { return CODEC; }
        @Override public void resolveDependencies(Resolver resolver) { locations(false).forEach(resolver::markDependency); }
        @Override public BlockStateModel bake(ModelBaker baker) {
            var manager = IForestryClientApi.INSTANCE.getTreeManager();
            Map<Identifier, BlockStateModelPart> parts = new HashMap<>();
            locations(false).forEach(id -> parts.put(id, new Variant(id).bake(baker)));
            BlockStateModelPart fallback = parts.get(manager.getDefaultSaplingModels().getFirst());
            return new BlockStateModel() {
                private BlockStateModelPart part(BlockAndTintGetter level, BlockPos pos) {
                    var species = level.getModelData(pos).get(SaplingBlockEntity.TREE_SPECIES);
                    return species == null ? fallback : parts.getOrDefault(manager.getSaplingModels(species).getFirst(), fallback);
                }
                @Override public void collectParts(RandomSource random, List<BlockStateModelPart> output) { output.add(fallback); }
                @Override public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> output) { output.add(part(level, pos)); }
                @Override public Material.Baked particleMaterial() { return fallback.particleMaterial(); }
                @Override public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) { return part(level, pos).particleMaterial(); }
                @Override public int materialFlags() { return fallback.materialFlags(); }
                @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) { return part(level, pos).materialFlags(); }
                @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return part(level, pos); }
            };
        }
    }
    public record Item() implements ItemModel.Unbaked {
        public static final MapCodec<Item> CODEC = MapCodec.unit(new Item());
        @Override public MapCodec<Item> type() { return CODEC; }
        @Override public void resolveDependencies(Resolver resolver) { locations(true).forEach(resolver::markDependency); }
        @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
            var manager = IForestryClientApi.INSTANCE.getTreeManager();
            Map<Identifier, ItemModel> models = new HashMap<>();
            for (Identifier id : locations(true)) models.put(id, new CuboidItemModelWrapper.Unbaked(id, Optional.empty(), java.util.stream.IntStream.range(0, 5).mapToObj(i -> (net.minecraft.client.color.item.ItemTintSource) new ForestryItemTintSource(i)).toList()).bake(context, transform));
            ItemModel fallback = models.get(manager.getDefaultSaplingModels().getSecond());
            return (output, stack, resolver, display, level, owner, seed) -> {
                var individual = IIndividualHandlerItem.getIndividual(stack);
                var selected = individual != null && individual.getSpecies() instanceof ITreeSpecies species
                    ? models.getOrDefault(manager.getSaplingModels(species).getSecond(), fallback) : fallback;
                selected.update(output, stack, resolver, display, level, owner, seed);
            };
        }
    }
}
