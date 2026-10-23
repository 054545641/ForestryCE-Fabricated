package forestry.lepidopterology.client;

import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;

import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import java.util.*;

/** Species texture and genome size both participate in the native item render identity. */
public record ButterflyItemModel() implements ItemModel.Unbaked {
    public static final MapCodec<ButterflyItemModel> CODEC = MapCodec.unit(new ButterflyItemModel());
    private static final Identifier BASE = ForestryConstants.forestry("item/butterfly_base");
    @Override public MapCodec<ButterflyItemModel> type() { return CODEC; }
    @Override public void resolveDependencies(Resolver resolver) { resolver.markDependency(BASE); }
    @Override public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transform) {
        var manager = ButterflyTextureCatalog.INSTANCE;
        var baker = context.blockModelBaker();
        var base = baker.getModel(BASE);
        Set<Identifier> textures = new HashSet<>();
        manager.getAllTextures().forEach(pair -> textures.add(pair.getFirst()));
        Identifier fallback = manager.getDefaultTextures().getFirst();
        textures.add(fallback);
        Map<Identifier, QuadCollection> geometry = new HashMap<>();
        Map<Identifier, ModelRenderProperties> properties = new HashMap<>();
        for (Identifier texture : textures) {
            var slots = new TextureSlots.Resolver().addLast(new TextureSlots.Data.Builder()
                .addTexture("butterfly", new Material(texture)).addTexture("particle", new Material(texture)).build()).resolve(() -> "Forestry butterfly");
            geometry.put(texture, base.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY));
            properties.put(texture, ModelRenderProperties.fromResolvedModel(baker, base, slots));
        }
        record Key(Identifier texture, float size) {}
        var cache = com.google.common.cache.CacheBuilder.newBuilder().maximumSize(256).<Key, ItemModel>build();
        return (output, stack, resolver, display, level, owner, seed) -> {
            var individual = IIndividualHandlerItem.getIndividual(stack);
            Identifier texture = fallback;
            float size = 1;
            if (individual != null) {
                var genome = individual.getGenome();
                texture = manager.getTextures(genome.resolveActive(ButterflyChromosomes.SPECIES)).getFirst();
                size = genome.getActiveValue(ButterflyChromosomes.SIZE);
            }
            if (!geometry.containsKey(texture)) texture = fallback;
            if (!Float.isFinite(size) || size <= 0) size = 1;
            Key key = new Key(texture, size);
            ItemModel model = cache.getIfPresent(key);
            if (model == null) {
                Matrix4f scaled = new Matrix4f(transform).translate(0.5f, 0.5f, 0.5f).scale(size).translate(-0.5f, -0.5f, -0.5f);
                model = new CuboidItemModelWrapper(List.of(), geometry.get(texture), properties.get(texture), scaled);
                cache.put(key, model);
            }
            model.update(output, stack, resolver, display, level, owner, seed);
        };
    }
}
