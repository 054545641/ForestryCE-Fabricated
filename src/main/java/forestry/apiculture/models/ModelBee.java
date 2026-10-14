package forestry.apiculture.models;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.client.IForestryClientApi;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.models.ForestryItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import java.util.*;

/** Native item selection keeps every life-stage model in the resource dependency graph. */
public record ModelBee(BeeLifeStage stage) implements ItemModel.Unbaked {
    public static final MapCodec<ModelBee> CODEC = Codec.STRING.comapFlatMap(name -> {
        try { return DataResult.success(BeeLifeStage.valueOf(name.toUpperCase(Locale.ROOT))); }
        catch (IllegalArgumentException exception) { return DataResult.error(() -> "Unknown bee life stage " + name); }
    }, stage -> stage.name().toLowerCase(Locale.ROOT)).fieldOf("stage").xmap(ModelBee::new, ModelBee::stage);
    @Override public MapCodec<ModelBee> type() { return CODEC; }
    private Set<Identifier> locations() {
        var manager = IForestryClientApi.INSTANCE.getBeeManager();
        Set<Identifier> locations = new HashSet<>(manager.getAllModelLocations(this.stage));
        locations.add(manager.getDefaultModelLocation(this.stage));
        return locations;
    }
    @Override public void resolveDependencies(Resolver resolver) { locations().forEach(resolver::markDependency); }
    @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
        var manager = IForestryClientApi.INSTANCE.getBeeManager();
        Map<Identifier, ItemModel> models = new HashMap<>();
        for (Identifier id : locations()) models.put(id, new CuboidItemModelWrapper.Unbaked(id, Optional.empty(),
            java.util.stream.IntStream.range(0, 5).mapToObj(i -> (net.minecraft.client.color.item.ItemTintSource) new ForestryItemTintSource(i)).toList()).bake(context, transform));
        ItemModel fallback = models.get(manager.getDefaultModelLocation(this.stage));
        return (output, stack, resolver, display, level, owner, seed) -> {
            var individual = IIndividualHandlerItem.getIndividual(stack);
            var selected = individual == null ? fallback : models.getOrDefault(manager.getModelLocation(this.stage, individual.getSpecies().id()), fallback);
            selected.update(output, stack, resolver, display, level, owner, seed);
        };
    }
}
