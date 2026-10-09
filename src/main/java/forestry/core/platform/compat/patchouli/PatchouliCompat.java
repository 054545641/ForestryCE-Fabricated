package forestry.core.platform.compat.patchouli;

import forestry.core.manual.ManualAccess;

public final class PatchouliCompat {
	private PatchouliCompat() {
	}

	// call only when ModList reports patchouli: loading PatchouliManualAccess resolves vazkii classes
	public static void init() {
		ManualAccess.install(new PatchouliManualAccess());
	}
}
