package forestry.core.content.burnbarrel;

import forestry.core.features.CoreMenuTypes;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.gui.slots.SlotFiltered;
import forestry.core.platform.inventory.InventoryBurnBarrel;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class ContainerBurnBarrel extends ContainerTile<TileBurnBarrel> {
	private static final int[] INPUT_X_COORDS = {70, 88, 70, 88};
	private static final int[] INPUT_Y_COORDS = {24, 24, 42, 42};
	private static final int[] OUTPUT_X_COORDS = {61, 79, 97};

	private int lastAshProgress = 0;
	private int lastBurnTime = 0;
	private int lastMaxBurnTime = 0;

	public static ContainerBurnBarrel fromNetwork(int windowId, Inventory playerInv, FriendlyByteBuf extraData) {
		TileBurnBarrel burnBarrel = TileUtil.getTile(playerInv.player.level(), extraData.readBlockPos(), TileBurnBarrel.class);
		return new ContainerBurnBarrel(windowId, playerInv, burnBarrel);
	}

	// Deviation from 1.20.1: that tree also called addDataSlots(new SimpleContainerData(8)). Nothing wrote to or read
	// from those slots, so they only broadcast eight zeroes. The barrel syncs through PacketGuiStream instead
	public ContainerBurnBarrel(int windowId, Inventory player, TileBurnBarrel tile) {
		super(windowId, CoreMenuTypes.BURN_BARREL.menuType(), player, tile, 8, 120);

		// Input buffer
		for (int i = 0; i < INPUT_X_COORDS.length; i++) {
			this.addSlot(new SlotFiltered(tile, InventoryBurnBarrel.SLOT_INPUT_1 + i, INPUT_X_COORDS[i], INPUT_Y_COORDS[i]));
		}

		// Output buffer
		for (int i = 0; i < OUTPUT_X_COORDS.length; i++) {
			this.addSlot(new SlotFiltered(tile, InventoryBurnBarrel.SLOT_OUTPUT_1 + i, OUTPUT_X_COORDS[i], 90));
		}
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		int currentAshProgress = this.tile.getAshProductionTimer();
		int currentBurnTime = this.tile.getBurnTime();
		int currentMaxBurnTime = this.tile.getCurrentMaxBurnTime();

		if (currentAshProgress != this.lastAshProgress
			|| currentBurnTime != this.lastBurnTime
			|| currentMaxBurnTime != this.lastMaxBurnTime) {
			this.lastAshProgress = currentAshProgress;
			this.lastBurnTime = currentBurnTime;
			this.lastMaxBurnTime = currentMaxBurnTime;

			PacketGuiStream packet = new PacketGuiStream(this.containerId, this.tile);
			sendPacketToListeners(packet);
		}
	}
}
