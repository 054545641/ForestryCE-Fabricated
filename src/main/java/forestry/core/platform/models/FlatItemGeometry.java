package forestry.core.platform.models;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import java.util.Map;

/** Front and back planes without extruded pixel edges, used for crate contents and container liquids. */
public final class FlatItemGeometry {
    private FlatItemGeometry() {}
    public static CuboidModelElement element(int tint, float x0, float y0, float x1, float y1, float offset) {
        return new CuboidModelElement(new Vector3f(x0, y0, 7.5f - offset), new Vector3f(x1, y1, 8.5f + offset), Map.of(
            Direction.SOUTH, new CuboidFace(null, tint, "#layer" + tint, new CuboidFace.UVs(0, 0, 16, 16), Quadrant.R0),
            Direction.NORTH, new CuboidFace(null, tint, "#layer" + tint, new CuboidFace.UVs(16, 0, 0, 16), Quadrant.R0)
        ), null, false, 0);
    }
    public static QuadCollection bake(ModelBaker baker, BakedQuad.MaterialInfo material, ModelState state,
                                      float x0, float y0, float x1, float y1, float offset) {
        var element = element(material.tintIndex(), x0, y0, x1, y1, offset);
        var quads = new QuadCollection.Builder();
        element.faces().forEach((direction, face) -> quads.addUnculledFace(FaceBakery.bakeQuad(baker.interner(),
            element.from(), element.to(), face.uvs(), face.rotation(), material, direction, state, null, face.faceData())));
        return quads.build();
    }
}
