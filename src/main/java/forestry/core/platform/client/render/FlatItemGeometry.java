package forestry.core.platform.client.render;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.texture.SpriteContents;
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
            element.from(), element.to(), face.uvs(), face.rotation(), material, direction, state, null)));
        return quads.build();
    }

    public static QuadCollection masked(ModelBaker baker, BakedQuad.MaterialInfo material, ModelState state,
                                         SpriteContents mask, boolean bucket, float offset) {
        var quads = new QuadCollection.Builder();
        int width = bucket ? 16 : mask.width();
        int height = bucket ? 16 : mask.height();
        for (int row = 0; row < height; row++) {
            int column = 0;
            while (column < width) {
                while (column < width && !filled(mask, bucket, column, row)) column++;
                int start = column;
                while (column < width && filled(mask, bucket, column, row)) column++;
                if (start == column) continue;
                float left = start * 16.0f / width;
                float right = column * 16.0f / width;
                float top = row * 16.0f / height;
                float bottom = (row + 1) * 16.0f / height;
                var from = new Vector3f(left, 16 - bottom, 7.5f - offset);
                var to = new Vector3f(right, 16 - top, 8.5f + offset);
                quads.addUnculledFace(FaceBakery.bakeQuad(baker.interner(), from, to,
                    new CuboidFace.UVs(left, top, right, bottom), Quadrant.R0, material, Direction.SOUTH, state, null));
                quads.addUnculledFace(FaceBakery.bakeQuad(baker.interner(), from, to,
                    new CuboidFace.UVs(right, top, left, bottom), Quadrant.R0, material, Direction.NORTH, state, null));
            }
        }
        return quads.build();
    }

    private static boolean filled(SpriteContents mask, boolean bucket, int column, int row) {
        if (!bucket) return !mask.isTransparent(0, column, row);
        return switch (row) {
            case 3 -> column >= 4 && column < 12;
            case 4 -> column >= 3 && column < 13;
            case 5 -> column >= 5 && column < 11;
            default -> false;
        };
    }
}
