package forestry.core.platform.gui;

import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.tile.ILiquidTankTile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import forestry.core.platform.fluids.StandardTank;

import javax.annotation.Nullable;

public abstract class ContainerLiquidTanksSocketed<T extends BlockEntity & ILiquidTankTile & ISocketable> extends ContainerTile<T> implements IContainerSocketed, IContainerLiquidTanks {
	private final ContainerSocketedHelper<T> socketedHelper;
	private final ContainerLiquidTanksHelper<T> tanksHelper;

	protected ContainerLiquidTanksSocketed(int windowId, MenuType<?> type, Inventory playerInventory, T tile, int xInv, int yInv) {
		super(windowId, type, playerInventory, tile, xInv, yInv);
		this.socketedHelper = new ContainerSocketedHelper<>(this.tile);
		this.tanksHelper = new ContainerLiquidTanksHelper<>(this.tile);
		if (this.player != null) {
			this.tile.getTankManager().sendAllTanks(this, this.player);
		}
	}

	/* IContainerLiquidTanks */
	@Override
	public void handlePipetteClickClient(int slot, Player player) {
        this.tanksHelper.handlePipetteClickClient(slot, player);
	}

	@Override
	public void handlePipetteClick(int slot, ServerPlayer player) {
        this.tanksHelper.handlePipetteClick(slot, player);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		if (this.player != null) {
            this.tile.getTankManager().broadcastChanges(this, this.player);
		}
	}

	@Override
	public void removed(Player PlayerEntity) {
		super.removed(PlayerEntity);
        this.tile.getTankManager().onClosed(this);
	}

	@Nullable
	@Override
	public StandardTank getTank(int slot) {
		return this.tile.getTankManager().getTank(slot);
	}

	/* IContainerSocketed */
	@Override
	public void handleChipsetClick(int slot) {
        this.socketedHelper.handleChipsetClick(slot);
	}

	@Override
	public void handleChipsetClickServer(int slot, ServerPlayer player, ItemStack itemstack) {
        this.socketedHelper.handleChipsetClickServer(slot, player, itemstack);
	}

	@Override
	public void handleSolderingIronClick(int slot) {
        this.socketedHelper.handleSolderingIronClick(slot);
	}

	@Override
	public void handleSolderingIronClickServer(int slot, ServerPlayer player, ItemStack itemstack) {
        this.socketedHelper.handleSolderingIronClickServer(slot, player, itemstack);
	}

}
