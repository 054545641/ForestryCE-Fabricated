package forestry.apiculture.alveary.multiblock;

import forestry.api.apiculture.IBeeListener;
import forestry.api.core.genetics.pollen.IPollen;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.apiculture.alveary.AlvearySieveMenu;
import forestry.apiculture.alveary.AlvearySieveInventory;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.inventory.watchers.ISlotPickupWatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class AlvearySieveBlockEntity extends AbstractAlvearyBlockEntity implements IAlvearyComponent.BeeListener<AlvearyMultiblockLogic>, IAlvearyComponent.HasInventory {
	private final IBeeListener beeListener;
	private final AlvearySieveInventory inventory;

	public AlvearySieveBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.SIEVE, pos, state);
		this.inventory = new AlvearySieveInventory(this);
		this.beeListener = new AlvearySieveBeeListener(this.inventory);
	}

	@Override
	public IInventoryAdapter getInternalInventory() {
		return this.inventory;
	}

	public ISlotPickupWatcher getCrafter() {
		return this.inventory;
	}

	@Override
	public IBeeListener getBeeListener() {
		return this.beeListener;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new AlvearySieveMenu(windowId, inv, this);
	}

	static class AlvearySieveBeeListener implements IBeeListener {
		private final AlvearySieveInventory inventory;

		public AlvearySieveBeeListener(AlvearySieveInventory inventory) {
			this.inventory = inventory;
		}

		@Override
		public boolean onPollenRetrieved(IPollen<?> pollen) {
			if (!this.inventory.canStorePollen()) {
				return false;
			}

			ItemStack pollenStack = pollen.createStack();
			if (!pollenStack.isEmpty()) {
				this.inventory.storePollenStack(pollenStack);
				return true;
			}
			return false;
		}
	}
}
