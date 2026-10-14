package forestry.arboriculture.client;

import com.mojang.blaze3d.platform.NativeImage;
import forestry.api.ForestryConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import java.io.IOException;
import java.util.List;

/** Adapts legacy sign sheets to the native edit-screen layout at reload time. Pack-provided editor images take precedence. */
public final class SignEditorTexture extends ReloadableTexture {
	private final String wood;
	private final boolean hanging;

	public SignEditorTexture(String wood, boolean hanging) {
		super(editorId(wood, hanging));
		this.wood = wood;
		this.hanging = hanging;
	}

	public static Identifier editorId(String wood, boolean hanging) {
		return ForestryConstants.forestry("textures/gui/" + (hanging ? "hanging_signs/" : "signs/") + wood + ".png");
	}

	public static void register(AddClientReloadListenersEvent event, List<String> woods) {
		var id = ForestryConstants.forestry("sign_editor_textures");
		event.addListener(id, (state, tasks, barrier, executor) -> barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
			var textures = Minecraft.getInstance().getTextureManager();
			for (String wood : woods) for (boolean hanging : new boolean[]{false, true}) {
				var texture = new SignEditorTexture(wood, hanging);
				textures.registerAndLoad(texture.resourceId(), texture);
			}
		}, executor));
		event.addDependency(VanillaClientListeners.TEXTURES, id);
	}

	@Override public TextureContents loadContents(ResourceManager resources) throws IOException {
		if (resources.getResource(resourceId()).isPresent()) return TextureContents.load(resources, resourceId());
		String legacy = BoatRenderGeometry.texture(this.wood, false).getPath().replace("textures/entity/boat/", "").replace(".png", "");
		if (this.hanging) {
			var editor = editorId(legacy, true);
			if (resources.getResource(editor).isPresent()) return TextureContents.load(resources, editor);
		}
		var sourceId = ForestryConstants.forestry("textures/entity/signs/" + (this.hanging ? "hanging/" : "") + legacy + ".png");
		try (var source = TextureContents.load(resources, sourceId)) {
			NativeImage image = compose(source.image(), this.hanging);
			return new TextureContents(image, source.metadata());
		}
	}

	public static NativeImage compose(NativeImage source, boolean hanging) throws IOException {
		// Preserve integer-scaled resource packs rather than truncating them to the default resolution.
		int scale = source.getWidth() / 64;
		if (scale < 1 || source.getWidth() != 64 * scale || source.getHeight() != 32 * scale) {
			throw new IOException("Expected a 64x32 sign sheet or an integer multiple, got " + source.getWidth() + "x" + source.getHeight());
		}
		NativeImage target = new NativeImage((hanging ? 16 : 24) * scale, (hanging ? 16 : 26) * scale, true);
		target.fillRect(0, 0, target.getWidth(), target.getHeight(), 0);
		if (hanging) {
			copy(source, target, 14, 6, 2, 0, 12, 6, scale);
			copy(source, target, 2, 14, 1, 6, 14, 10, scale);
		} else {
			copy(source, target, 2, 2, 0, 0, 24, 12, scale);
			copy(source, target, 2, 16, 11, 12, 2, 14, scale);
		}
		return target;
	}

	private static void copy(NativeImage source, NativeImage target, int sx, int sy, int dx, int dy, int w, int h, int scale) {
		for (int y = 0; y < h * scale; y++) for (int x = 0; x < w * scale; x++) {
			target.setPixel(dx * scale + x, dy * scale + y, source.getPixel(sx * scale + x, sy * scale + y));
		}
	}
}
