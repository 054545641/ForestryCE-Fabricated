package forestry.core.data;

import java.nio.file.Path;

import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * The generated resource root of each jar. The data run's output folder is the parent of every root,
 * so each one is derived from the run rather than assumed relative to a working directory.
 */
public final class DataRoots {
	public static final String CORE = "resources";
	public static final String FARMS = "resources_farms";
	public static final String MAIL = "resources_mail";
	public static final String BUTTERFLIES = "resources_butterflies";

	private DataRoots() {
	}

	/**
	 * @param event     The gather event the run's output folder is read from
	 * @param directory The root directory name, one of the constants above
	 * @return The pack output every provider belonging to that jar writes to
	 */
	public static PackOutput of(GatherDataEvent event, String directory) {
		Path root = event.getGenerator().getPackOutput().getOutputFolder();
		return new PackOutput(root.resolve(directory));
	}
}
