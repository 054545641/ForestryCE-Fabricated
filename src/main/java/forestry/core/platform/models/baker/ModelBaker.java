package forestry.core.platform.models.baker;

import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import com.mojang.math.Quadrant;
import forestry.core.platform.util.ResourceUtil;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;


/**
 * A model baker to make custom block models with more than one texture layer.
 */
//Todo: Test if baker can be replaced with model loaders
public final class ModelBaker {

    // Runtime leaf baking must not retain mutable vertex vectors owned by callers.
    private static final net.minecraft.client.resources.model.ModelBaker.Interner INTERNER = new net.minecraft.client.resources.model.ModelBaker.Interner() {
        @Override public org.joml.Vector3fc vector(org.joml.Vector3fc value) { return new Vector3f(value); }
        @Override public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo value) { return value; }
    };
	private static final Vector3f POS_FROM = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final Vector3f POS_TO = new Vector3f(16.0F, 16.0F, 16.0F);

	private final List<ModelBakerFace> faces = new ArrayList<>();

	private TextureAtlasSprite particleSprite;

	private int colorIndex = -1;

	public ModelBaker addBlockModel(TextureAtlasSprite[] textures, int colorIndex) {
		this.colorIndex = colorIndex;

		for (Direction facing : Direction.values()) {
			addFace(facing, textures[facing.ordinal()]);
		}
		return this;
	}

	public ModelBaker addBlockModel(TextureAtlasSprite texture, int colorIndex) {
		return addBlockModel(new TextureAtlasSprite[]{texture, texture, texture, texture, texture, texture}, colorIndex);
	}

	public ModelBaker addFace(Direction facing, TextureAtlasSprite sprite) {
		this.faces.add(new ModelBakerFace(facing, this.colorIndex, sprite));
		return this;
	}

    public ModelBakerModel bake(boolean flip) {
        net.minecraft.client.renderer.block.dispatch.ModelState rotation = BlockModelRotation.IDENTITY;
        if (flip) rotation = new net.neoforged.neoforge.client.model.ComposedModelState(rotation,
            new com.mojang.math.Transformation(null, new org.joml.Quaternionf().rotationY((float) Math.PI), null, null));
        QuadCollection.Builder quads = new QuadCollection.Builder();
        for (ModelBakerFace face : this.faces) {
            Material.Baked material = new Material.Baked(face.spite, false);
            var info = BakedQuad.MaterialInfo.of(material, face.spite.contents().transparency(), face.colorIndex, true, 0, true);
            BakedQuad quad = FaceBakery.bakeQuad(INTERNER, POS_FROM, POS_TO, new CuboidFace.UVs(0, 0, 16, 16),
                Quadrant.R0, info, face.face, rotation, null, net.neoforged.neoforge.client.model.ExtraFaceData.DEFAULT);
            // Cull using the transformed normal, not the source face (items are rotated 180 degrees).
            quads.addCulledFace(quad.direction(), quad);
        }
        return new ModelBakerModel(quads.build(), new Material.Baked(this.particleSprite == null ? ResourceUtil.getMissingTexture() : this.particleSprite, false));
    }

    public void setParticleSprite(TextureAtlasSprite sprite) { this.particleSprite = sprite; }
}
