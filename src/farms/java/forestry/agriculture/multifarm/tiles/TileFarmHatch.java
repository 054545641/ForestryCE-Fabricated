package forestry.agriculture.multifarm.tiles;

import forestry.api.core.multiblock.IFarmComponent;
import forestry.core.platform.inventory.AdjacentInventoryCache;
import forestry.core.platform.tile.AdjacentTileCache;
import forestry.core.platform.util.InventoryUtil;
import forestry.agriculture.features.MultifarmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import forestry.core.platform.inventory.InventoryTransferView;

import javax.annotation.Nullable;

public class TileFarmHatch extends TileFarm implements WorldlyContainer, IFarmComponent.Active {

	private final AdjacentTileCache tileCache;
	private final AdjacentInventoryCache inventoryCache;

	public TileFarmHatch(BlockPos pos, BlockState state) {
		super(MultifarmBlockEntities.HATCH.tileType(), pos, state);
		this.tileCache = new AdjacentTileCache(this);
		this.inventoryCache = new AdjacentInventoryCache(this, this.tileCache, tile -> !(tile instanceof TileFarm) && tile.getBlockPos().getY() < getBlockPos().getY());
	}

	@Override
	public boolean allowsAutomation() {
		return true;
	}

	@Override
	public void updateServer(int tickCount) {
		if (tickCount % 40 == 0) {
			Container productInventory = getMultiblockLogic().getController().getFarmInventory().getProductInventory();
			SlottedStorage<ItemVariant> productItemHandler = InventoryTransferView.internal(productInventory);

			InventoryUtil.moveItemStack(productItemHandler, this.inventoryCache.getAdjacentInventories());
		}
	}

	@Override
	public void updateClient(int tickCount) {

	}

	@Nullable
	public SlottedStorage<ItemVariant> getItemHandler(@Nullable Direction facing) {
		return InventoryTransferView.automation(this, facing);
	}
}
