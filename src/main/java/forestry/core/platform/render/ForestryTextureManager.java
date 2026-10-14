package forestry.core.platform.render;

import forestry.api.client.ITextureManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public class ForestryTextureManager implements ITextureManager {
	private final ForestrySpriteUploader uploader = new ForestrySpriteUploader();

	public ForestrySpriteUploader getSpriteUploader() {
		return this.uploader;
	}

	@Override
	public TextureAtlasSprite getSprite(Identifier location) {
		return this.uploader.getSprite(location);
	}
}
