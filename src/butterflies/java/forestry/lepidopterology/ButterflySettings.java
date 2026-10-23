package forestry.lepidopterology;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
/** Original gameplay defaults, independent of loader lifecycle. */
public final class ButterflySettings {
	public static int maxDistance = 64;
	private static final boolean allowPollination = true;
	public static final Object2FloatOpenHashMap<String> spawnRarities = new Object2FloatOpenHashMap<>();
	public static boolean spawnButterflysFromLeaves = true;
	private static final boolean generateCocoons = false;
	private static final float generateCocoonsAmount = 1.0f;
	private static final float serumChance = 0.55f;
	private static final float secondSerumChance = 0;
	public static boolean isPollinationAllowed() {
		return allowPollination;
	}

	public static boolean isSpawnButterflysFromLeaves() {
		return spawnButterflysFromLeaves;
	}

	public static boolean isGenerateCocoons() {
		return generateCocoons;
	}

	public static float getGenerateCocoonsAmount() {
		return generateCocoonsAmount;
	}

	public static float getSerumChance() {
		return serumChance;
	}

	public static float getSecondSerumChance() {
		return secondSerumChance;
	}

}
