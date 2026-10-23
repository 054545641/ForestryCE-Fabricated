package forestry.core.platform.compat.curios;

import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosCompat implements forestry.core.platform.compat.NaturalistEyeAccess {
	public static final boolean IS_LOADED = ModList.get().isLoaded("curios");

	@Override
	public boolean hasNaturalistEye(Player player) {
		if (!IS_LOADED) return false;
		return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.getStacksHandler("head").map(handler -> {
			top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks = handler.getStacks();

			for (int i = 0; i < stacks.getSlots(); i++) {
				if (GeneticsUtil.hasNaturalistEye(player, stacks.getStackInSlot(i))) {
					return true;
				}
			}

			return false;
		}).orElse(false)).orElse(false);
	}
}
