package forestry.core.platform.data;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import forestry.api.IForestryApi;
import forestry.api.apiculture.*;
import forestry.api.apiculture.genetics.IBeeEffect;
import forestry.apiimpl.GeneticManager;
import forestry.apiculture.bees.genetics.*;
import forestry.core.engine.genetics.*;
import net.fabricmc.fabric.api.resource.v1.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Stages definitions in the reload-owned store; runtime publication only happens after a successful reload. */
public final class GeneticDataLoader implements PreparableReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("forestry", "genetic_data");
    public static final DataResourceStore.Key<Definitions> KEY = new DataResourceStore.Key<>();
    public static final List<String> FOLDERS = List.of("taxon", "flower_type", "bee_effect", "bee_jubilance", "bee_species", "tree_species", "mutations");
    private static final Map<String, Extension<?>> EXTENSIONS = new LinkedHashMap<>();
    private static final List<Runnable> AFTER_PROJECTION = new ArrayList<>();
    public static void afterProjection(Runnable builder) { AFTER_PROJECTION.add(Objects.requireNonNull(builder)); }
    private static final List<Runnable> AFTER_APPLY = new ArrayList<>();
    public static void afterApply(Runnable listener) { AFTER_APPLY.add(Objects.requireNonNull(listener)); }
    public record Extension<T>(java.util.function.Supplier<Codec<T>> codec, java.util.function.Consumer<Map<Identifier, T>> publish) {}
    public record ExtensionValues<T>(Extension<T> extension, Map<Identifier, T> values) { void apply() { extension.publish.accept(values); } }
    public static <T> void registerFolder(String folder, java.util.function.Supplier<Codec<T>> codec, java.util.function.Consumer<Map<Identifier, T>> publish) {
        if (FOLDERS.contains(folder) || EXTENSIONS.putIfAbsent(folder, new Extension<>(codec, publish)) != null) throw new IllegalArgumentException("Duplicate genetic folder " + folder);
    }
    private static List<String> folders() { var folders = new ArrayList<>(FOLDERS); folders.addAll(EXTENSIONS.keySet()); return List.copyOf(folders); }
    public record Definitions(Map<Identifier, TaxonDefinition> taxa, Map<Identifier, IFlowerType> flowers,
            Map<Identifier, IBeeEffect> effects, Map<Identifier, IBeeJubilance> jubilances, Map<Identifier, BeeSpeciesDefinition> bees,
            Map<Identifier, forestry.arboriculture.trees.genetics.TreeSpeciesDefinition> trees,
            Map<Identifier, forestry.core.engine.genetics.mutations.MutationRecipe> mutations, List<ExtensionValues<?>> extensions, JsonObject wire, LogicalData.Update prepared) {
        Definitions withPrepared(LogicalData.Update update) { return new Definitions(taxa, flowers, effects, jubilances, bees, trees, mutations, extensions, wire, update); }
    }
    @Override public CompletableFuture<Void> reload(SharedState state, Executor prepare, PreparationBarrier barrier, Executor apply) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject root = new JsonObject();
            for (String folder : folders()) {
                String resourceFolder = folder.equals("mutations") ? "recipe" : folder;
                JsonObject definitions = new JsonObject();
                state.resourceManager().listResources(resourceFolder, path -> path.getPath().endsWith(".json")).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                        try (var input = entry.getValue().open()) {
                            byte[] bytes = input.readNBytes(DataMapLoader.MAX_DOCUMENT_BYTES + 1);
                            if (bytes.length > DataMapLoader.MAX_DOCUMENT_BYTES) throw new IllegalArgumentException("Genetic definition is too large");
                            var file = entry.getKey();
                            var id = file.withPath(file.getPath().substring(resourceFolder.length() + 1, file.getPath().length() - 5));
                            JsonObject json = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                            if (folder.equals("mutations") && (!json.has("type") || !Set.of("forestry:bee_mutation", "forestry:tree_mutation", "forestry:butterfly_mutation").contains(json.get("type").getAsString()))) return;
                            definitions.add(id.toString(), json);
                        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
                    });
                root.add(folder, definitions);
            }
            return root;
        }, prepare).thenCompose(barrier::wait).thenAcceptAsync(root ->
            state.get(DataResourceLoader.DATA_RESOURCE_STORE_KEY).put(KEY, decode(root, state.get(ResourceLoader.REGISTRY_LOOKUP_KEY), true)), apply);
    }
    public static Definitions decode(JsonObject root, HolderLookup.Provider lookup) {
        return decode(root, lookup, LogicalData.isServer());
    }
    private static Definitions decode(JsonObject root, HolderLookup.Provider lookup, boolean serverSide) {
        if (!root.keySet().equals(new HashSet<>(folders()))) throw new IllegalArgumentException("Incompatible genetic definition set");
        var definitions = new Definitions(parse(root, "taxon", TaxonDefinition.CODEC, lookup), parse(root, "flower_type", IFlowerType.CODEC, lookup),
            parse(root, "bee_effect", IBeeEffect.CODEC, lookup), parse(root, "bee_jubilance", IBeeJubilance.CODEC, lookup),
            parse(root, "bee_species", BeeSpeciesDefinition.codec(), lookup),
            parse(root, "tree_species", forestry.arboriculture.trees.genetics.TreeSpeciesDefinition.codec(), lookup),
            parseMutations(root, lookup), EXTENSIONS.entrySet().stream().<ExtensionValues<?>>map(entry -> decodeExtension(root, entry.getKey(), entry.getValue(), lookup)).toList(), root.deepCopy(), null);
        return definitions.withPrepared(LogicalData.prepare(serverSide, () -> project(definitions)));
    }
    private static <T> ExtensionValues<?> decodeExtension(JsonObject root, String folder, Extension<T> extension, HolderLookup.Provider lookup) {
        return new ExtensionValues<>(extension, parse(root, folder, extension.codec.get(), lookup));
    }
    private static Map<Identifier, forestry.core.engine.genetics.mutations.MutationRecipe> parseMutations(JsonObject root, HolderLookup.Provider lookup) {
        var definitions = root.getAsJsonObject("mutations");
        if (definitions == null || definitions.size() > 16_384) throw new IllegalArgumentException("Invalid mutations");
        Map<Identifier, forestry.core.engine.genetics.mutations.MutationRecipe> result = new TreeMap<>();
        definitions.entrySet().forEach(entry -> {
            String type = entry.getValue().getAsJsonObject().get("type").getAsString();
            Identifier speciesType = switch (type) {
                case "forestry:bee_mutation" -> forestry.api.core.genetics.ForestrySpeciesTypes.BEE;
                case "forestry:tree_mutation" -> forestry.api.core.genetics.ForestrySpeciesTypes.TREE;
                case "forestry:butterfly_mutation" -> forestry.api.core.genetics.ForestrySpeciesTypes.BUTTERFLY;
                default -> throw new IllegalArgumentException("Unknown mutation type " + type);
            };
            var codec = forestry.core.engine.genetics.mutations.MutationRecipe.createSerializer(speciesType).codec().codec();
            result.put(Identifier.parse(entry.getKey()), codec.parse(lookup.createSerializationContext(JsonOps.INSTANCE), entry.getValue()).getOrThrow());
        });
        return Map.copyOf(result);
    }
    private static <T> Map<Identifier, T> parse(JsonObject root, String folder, Codec<T> codec, HolderLookup.Provider lookup) {
        var definitions = root.getAsJsonObject(folder);
        if (definitions == null || definitions.size() > 16_384) throw new IllegalArgumentException("Invalid genetic definitions: " + folder);
        Map<Identifier, T> result = new TreeMap<>();
        definitions.entrySet().forEach(entry -> result.put(Identifier.parse(entry.getKey()),
            codec.parse(lookup.createSerializationContext(JsonOps.INSTANCE), entry.getValue()).getOrThrow()));
        return Map.copyOf(result);
    }
    public static void apply(Definitions definitions) {
        // Native item components are bound after the resource-loader phase. Farm icons/soil stacks
        // can therefore only be constructed now, still inside the unpublished candidate.
        definitions.prepared().complete(() -> AFTER_PROJECTION.forEach(Runnable::run)).publish();
        AFTER_APPLY.forEach(Runnable::run);
    }
    private static void project(Definitions definitions) {
        ((GeneticManager) IForestryApi.INSTANCE.getGeneticManager()).applyDatapackTaxa(definitions.taxa().values());
        ((ForestryFlowerTypeManager) IForestryApi.INSTANCE.getFlowerTypeManager()).rebuild(definitions.flowers());
        ApicultureReloadHandler.rebuildBeeEffects(definitions.effects());
        ApicultureReloadHandler.rebuildJubilances(definitions.jubilances());
        ApicultureReloadHandler.rebuildSpecies(definitions.bees());
        forestry.arboriculture.trees.genetics.ArboricultureReloadHandler.rebuildTreeSpecies(definitions.trees());
        definitions.extensions().forEach(ExtensionValues::apply);
        var recipes = definitions.mutations().entrySet().stream().<net.minecraft.world.item.crafting.RecipeHolder<?>>map(entry ->
            new net.minecraft.world.item.crafting.RecipeHolder<>(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, entry.getKey()), entry.getValue())).toList();
        GeneticsReloadHandler.rebuildMutations(net.minecraft.world.item.crafting.RecipeMap.create(recipes));
    }
    public static void clearClient() {
        LogicalData.prepare(false, GeneticDataLoader::clearDomains).publish();
        AFTER_APPLY.forEach(Runnable::run);
    }
    private static void clearDomains() {
        ((GeneticManager) IForestryApi.INSTANCE.getGeneticManager()).applyDatapackTaxa(List.of());
        ((ForestryFlowerTypeManager) IForestryApi.INSTANCE.getFlowerTypeManager()).rebuild(Map.of());
        ApicultureReloadHandler.rebuildBeeEffects(Map.of());
        ApicultureReloadHandler.rebuildJubilances(Map.of());
        ApicultureReloadHandler.rebuildSpecies(Map.of());
        forestry.arboriculture.trees.genetics.ArboricultureReloadHandler.rebuildTreeSpecies(Map.of());
        EXTENSIONS.values().forEach(extension -> extension.publish.accept(Map.of()));
        GeneticsReloadHandler.rebuildMutations(net.minecraft.world.item.crafting.RecipeMap.create(List.of()));
        ((forestry.apiimpl.ForestryApiImpl) IForestryApi.INSTANCE).setFarmingManager(forestry.apiimpl.fake.FakeFarmingManager.INSTANCE);
    }
}
