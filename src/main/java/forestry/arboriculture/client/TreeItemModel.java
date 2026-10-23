package forestry.arboriculture.client;

import com.mojang.serialization.MapCodec;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.arboriculture.leaves.ForestryLeafType;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import java.util.*;

/** Native item model selection keeps the original per-species sapling artwork. */
public record TreeItemModel() implements ItemModel.Unbaked {
    public static final MapCodec<TreeItemModel> CODEC = MapCodec.unit(new TreeItemModel());
    private static final Set<String> VANILLA = Set.of("oak", "birch", "spruce", "jungle", "acacia", "dark_oak", "cherry");
    private static Identifier model(Identifier species) {
        return Identifier.fromNamespaceAndPath(species.getNamespace().equals("forestry") && VANILLA.contains(species.getPath()) ? "minecraft" : species.getNamespace(), "item/" + species.getPath() + "_sapling");
    }
    @Override public MapCodec<TreeItemModel> type() { return CODEC; }
    @Override public void resolveDependencies(Resolver resolver) {
        for (var type : ForestryLeafType.allValues()) resolver.markDependency(model(type.getSpeciesId()));
    }
    @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
        var models = new HashMap<Identifier, ItemModel>();
        for (var type : ForestryLeafType.allValues()) models.put(type.getSpeciesId(), new CuboidItemModelWrapper.Unbaked(model(type.getSpeciesId()), Optional.empty(), List.of()).bake(context, transform));
        var fallback = Objects.requireNonNull(models.get(forestry.api.arboriculture.ForestryTreeSpecies.OAK));
        return (output, stack, resolver, display, level, owner, seed) -> {
            var individual = IIndividualHandlerItem.getIndividual(stack);
            var selected = individual != null && individual.getSpecies() instanceof ITreeSpecies species ? models.getOrDefault(species.id(), fallback) : fallback;
            selected.update(output, stack, resolver, display, level, owner, seed);
        };
    }
}
