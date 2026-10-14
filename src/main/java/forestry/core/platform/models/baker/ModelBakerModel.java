package forestry.core.platform.models.baker;

import forestry.core.platform.models.ForestryItemTintSource;
import forestry.core.platform.models.ModelTransforms;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import java.util.List;

/** An immutable geometry snapshot shared by native block parts and the item renderer. */
public final class ModelBakerModel implements BlockStateModelPart {
    private final QuadCollection quads;
    private final Material.Baked particle;
    private boolean ambientOcclusion;
    private final ItemModel itemModel;

    ModelBakerModel(QuadCollection quads, Material.Baked particle) {
        this.quads = quads;
        this.particle = particle;
        int maxTint = quads.getAll().stream().mapToInt(q -> q.materialInfo().tintIndex()).max().orElse(-1);
        List<net.minecraft.client.color.item.ItemTintSource> tints = java.util.stream.IntStream.rangeClosed(0, maxTint)
            .mapToObj(i -> (net.minecraft.client.color.item.ItemTintSource) new ForestryItemTintSource(i)).toList();
        this.itemModel = new CuboidItemModelWrapper(tints, quads, new ModelRenderProperties(true, particle, ModelTransforms.BLOCK), new Matrix4f());
    }
    public ItemModel asItemModel() { return this.itemModel; }
    public QuadCollection quads() { return this.quads; }
    public void setAmbientOcclusion(boolean value) { this.ambientOcclusion = value; }
    @Override public boolean useAmbientOcclusion() { return this.ambientOcclusion; }
    @Override public List<BakedQuad> getQuads(@Nullable Direction side) { return this.quads.getQuads(side); }
    @Override public Material.Baked particleMaterial() { return this.particle; }
    @Override public int materialFlags() { return this.quads.materialFlags(); }
}
