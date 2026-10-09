package forestry.core.manual;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

public final class ManualAccess {
	private static volatile IManualAccess instance = new MissingManualAccess();

	private ManualAccess() {
	}

	public static IManualAccess get() {
		return instance;
	}

	// called once from the provider's compat package, after the provider mod is detected
	public static void install(IManualAccess access) {
		instance = access;
	}

	// default so the manual item and tooltip degrade instead of failing without a provider
	private static final class MissingManualAccess implements IManualAccess {
		@Override
		public boolean isAvailable() {
			return false;
		}

		@Override
		public void openManual(ServerPlayer player, Item manualItem) {
		}
	}
}
