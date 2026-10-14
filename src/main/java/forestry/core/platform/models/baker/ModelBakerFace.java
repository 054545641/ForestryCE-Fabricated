package forestry.core.platform.models.baker;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * A face of a {@link ModelBakerModel }
 */
public class ModelBakerFace {
	public final Direction face;

	public final TextureAtlasSprite spite;

	public final int colorIndex;

	public ModelBakerFace(Direction face, int colorIndex, TextureAtlasSprite sprite) {
		this.colorIndex = colorIndex;
		this.face = face;
		this.spite = sprite;
	}

}
