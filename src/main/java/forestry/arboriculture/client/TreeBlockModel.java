package forestry.arboriculture.client;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.arboriculture.leaves.ForestryLeafType;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.*;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import java.util.*;
import java.util.function.Predicate;
/** Immutable native geometry selected from Fabric's block entity render-data snapshot. */
public record TreeBlockModel() implements BlockStateModel.Unbaked {
    private static final Set<String> VANILLA = Set.of("oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "cherry");
    private static Identifier model(Identifier species) { return Identifier.fromNamespaceAndPath(species.getNamespace().equals("forestry") && VANILLA.contains(species.getPath()) ? "minecraft" : species.getNamespace(), "block/" + species.getPath() + "_sapling"); }
    @Override public void resolveDependencies(Resolver resolver) { for (var type : ForestryLeafType.allValues()) resolver.markDependency(model(type.getSpeciesId())); }
    @Override public BlockStateModel bake(ModelBaker baker) {
        var models = new HashMap<Identifier, BlockStateModel>();
        for (var type : ForestryLeafType.allValues()) models.put(type.getSpeciesId(), new SingleVariant(new Variant(model(type.getSpeciesId())).bake(baker)));
        var fallback = Objects.requireNonNull(models.get(forestry.api.arboriculture.ForestryTreeSpecies.OAK));
        return new BlockStateModel() {
            private BlockStateModel selected(BlockAndTintGetter level, BlockPos pos) {
                var data = level.getBlockEntityRenderData(pos);
                return data instanceof ITreeSpecies species ? models.getOrDefault(species.id(), fallback) : fallback;
            }
            @Override public void collectParts(RandomSource random, List<BlockStateModelPart> parts) { fallback.collectParts(random, parts); }
            @Override public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cull) { selected(level, pos).emitQuads(emitter, level, pos, state, random, cull); }
            @Override public Material.Baked particleMaterial() { return fallback.particleMaterial(); }
            @Override public int materialFlags() { return fallback.materialFlags(); }
            @Override public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) { return selected(level, pos).particleMaterial(); }
            @Override public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return selected(level, pos).materialFlags(); }
            @Override public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) { return selected(level, pos); }
        };
    }
    public static void initialize() {
        net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin.register(context -> context.registerBlockStateResolver(forestry.arboriculture.features.ArboricultureBlocks.SAPLING_GE.block(), resolver -> {
            var model = new TreeBlockModel().asRoot();
            resolver.block().getStateDefinition().getPossibleStates().forEach(state -> resolver.setModel(state, model));
        }));
    }
}
