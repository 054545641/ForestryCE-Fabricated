package forestry.mail.postoffice;

import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.tile.TileBase;
import forestry.core.platform.util.InventoryUtil;
import forestry.mail.features.MailBlockEntities;
import forestry.mail.gui.StampCollectorMenu;
import forestry.mail.inventory.StampCollectorInventory;
import forestry.mail.letters.PostageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StampCollectorBlockEntity extends TileBase implements Container {
	public StampCollectorBlockEntity(BlockPos pos, BlockState state) {
		super(MailBlockEntities.STAMP_COLLECTOR.tileType(), pos, state);
		setInternalInventory(new StampCollectorInventory(this));
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		if (!updateOnInterval(20)) {
			return;
		}

		try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
			var office = PostOffice.getOrCreate((ServerLevel) level);
			var inventory = getInternalInventory();
			ItemStack filter = inventory.getItem(StampCollectorInventory.SLOT_FILTER);
			ItemStack stamp;
			if (filter.isEmpty()) stamp = office.getAnyStamp(1);
			else if (PostageUtil.isStamp(filter)) stamp = office.getAnyStamp(filter.getItem(), 1);
			else return;
			if (stamp.isEmpty()) return;
			if (!InventoryUtil.tryAddStack(inventory, stamp, StampCollectorInventory.SLOT_BUFFER_1, StampCollectorInventory.SLOT_BUFFER_COUNT, true)) return;
			tx.commit();
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new StampCollectorMenu(windowId, inv, this);
	}
}
