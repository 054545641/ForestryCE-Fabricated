package forestry.core.platform.gui;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface IContainerSocketed {
	void handleChipsetClick(int slot);

	void handleChipsetClickServer(int slot, ServerPlayer player, ItemStack itemstack);

	void handleSolderingIronClick(int slot);

	void handleSolderingIronClickServer(int slot, ServerPlayer player, ItemStack itemstack);
}
