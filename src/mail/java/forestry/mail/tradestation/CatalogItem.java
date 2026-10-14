package forestry.mail.tradestation;

import forestry.core.platform.item.WithScreenItem;
import forestry.mail.gui.CatalogMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CatalogItem extends WithScreenItem {
	public CatalogItem(Properties properties) {
		super(properties);
	}

	@Nullable
	@Override
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		return new CatalogMenu(containerId, player.getInventory());
	}
}
