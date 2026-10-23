package forestry.core.platform.client.render;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import forestry.api.ForestryConstants;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;

/** Crate geometry is baked within the current resource reload; no static sprite/model cache survives it. */
public final class FilledCrateModel {
    public static final Identifier BASE = ForestryConstants.forestry("item/filled_crate");
    public record Item(net.minecraft.resources.Identifier model) implements net.minecraft.client.renderer.item.ItemModel.Unbaked {
        public static final com.mojang.serialization.MapCodec<Item> CODEC = Identifier.CODEC.fieldOf("model").xmap(Item::new, Item::model);
        @Override public com.mojang.serialization.MapCodec<Item> type() { return CODEC; }
        @Override public void resolveDependencies(Resolver resolver) { resolver.markDependency(this.model); resolver.markDependency(BASE); }
        @Override public net.minecraft.client.renderer.item.ItemModel bake(net.minecraft.client.renderer.item.ItemModel.BakingContext context, org.joml.Matrix4fc transform) {
            var baker = context.blockModelBaker();
            var resolved = baker.getModel(this.model);
            var slots = resolved.getTopTextureSlots();
            var baseModel = baker.getModel(BASE);
            var state = net.minecraft.client.renderer.block.dispatch.BlockModelRotation.IDENTITY;
            var geometry = new ArrayList<CuboidModelElement>();
            for (int layer = 1; layer <= 4; layer++) if (slots.getMaterial("layer" + layer) != null) geometry.add(FlatItemGeometry.element(layer, 3, 4, 11, 12, 0.002f));
            var quads = new QuadCollection.Builder().addAll(baseModel.bakeTopGeometry(baseModel.getTopTextureSlots(), baker, state))
                .addAll(new UnbakedCuboidGeometry(geometry).bake(slots, baker, state, () -> "Forestry crate")).build();
            var properties = net.minecraft.client.renderer.item.ModelRenderProperties.fromResolvedModel(baker, baseModel, baseModel.getTopTextureSlots());
            // A crate can combine an item-atlas frame with block-atlas cargo. Each native layer owns one atlas.
            var layers = new java.util.LinkedHashMap<Identifier, QuadCollection.Builder>();
            for (var quad : quads.getAll()) layers.computeIfAbsent(quad.materialInfo().sprite().atlasLocation(), key -> new QuadCollection.Builder()).addUnculledFace(quad);
            java.util.List<net.minecraft.client.color.item.ItemTintSource> tints = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> (net.minecraft.client.color.item.ItemTintSource) new forestry.core.platform.models.ForestryItemTintSource(i)).toList();
            return new net.minecraft.client.renderer.item.CompositeModel(layers.values().stream()
                .map(layer -> (net.minecraft.client.renderer.item.ItemModel) new net.minecraft.client.renderer.item.CuboidItemModelWrapper(tints, layer.build(), properties, transform)).toList());
        }
    }
}
