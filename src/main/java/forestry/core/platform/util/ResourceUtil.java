package forestry.core.platform.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;

/**
 * Util methods used at the installation of the game or at the reloading or baking of resources like models or
 * textures.
 */
public class ResourceUtil {

	public static TextureAtlasSprite getMissingTexture() {
		return getSprite(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation());
	}

	public static TextureAtlasSprite getSprite(Identifier atlas, Identifier sprite) {
		return Minecraft.getInstance().getAtlasManager().get(new SpriteId(atlas, sprite));
	}

	public static TextureAtlasSprite getBlockSprite(Identifier location) {
		return getSprite(TextureAtlas.LOCATION_BLOCKS, location);
	}

	public static TextureAtlasSprite getBlockSprite(String location) {
		return getBlockSprite(Identifier.parse(location));
	}

}
