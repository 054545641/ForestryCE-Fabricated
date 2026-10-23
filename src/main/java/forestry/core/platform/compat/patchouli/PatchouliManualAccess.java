package forestry.core.platform.compat.patchouli;

import forestry.core.manual.IManualAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import vazkii.patchouli.api.PatchouliAPI;

public class PatchouliManualAccess implements IManualAccess {
	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public void openManual(ServerPlayer player, Item manualItem) {
		PatchouliAPI.get().openBookGUI(player, BuiltInRegistries.ITEM.getKey(manualItem));
	}
}
