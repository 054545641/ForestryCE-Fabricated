package forestry.core.platform.render;

import forestry.Forestry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Todo replace with a section in the client config
public enum ColourProperties implements ResourceManagerReloadListener {
	INSTANCE;

	private final Properties defaultMappings = new Properties();
	private final Properties mappings = new Properties();

	public int get(String key) {
		return Integer.parseInt(this.mappings.getProperty(key, this.defaultMappings.getProperty(key, "d67fff")), 16);
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		try (InputStream defaultFontStream = ColourProperties.class.getResourceAsStream("/config/forestry/colour.properties")) {
            if (defaultFontStream == null) throw new IOException("Missing Forestry colour defaults");
            this.defaultMappings.clear();
            this.defaultMappings.load(defaultFontStream);
            this.mappings.clear();
            this.mappings.putAll(this.defaultMappings);
		} catch (IOException e) {
			Forestry.LOGGER.error("Failed to load colors.properties.", e);
		}
	}

}
