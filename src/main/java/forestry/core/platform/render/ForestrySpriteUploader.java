package forestry.core.platform.render;

import forestry.api.ForestryConstants;
import forestry.api.client.ForestrySprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasRegistry;

/** Forestry GUI atlas access; the vanilla atlas manager owns loading and reloading. */
public final class ForestrySpriteUploader {
	public static final Identifier ATLAS_PATH = ForestryConstants.forestry("gui");

	public static void registerAtlas() {
		AtlasRegistry.register(new AtlasManager.AtlasConfig(ForestrySprites.TEXTURE_ATLAS, ATLAS_PATH, false, java.util.Set.of()));
	}

	public TextureAtlasSprite getSprite(Identifier location) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(ATLAS_PATH).getSprite(location);
	}
}
