package forestry.core.content.machines.gui;

import forestry.core.platform.gui.ContainerLiquidTanks;
import forestry.core.platform.gui.IContainerCrafting;
import forestry.core.platform.gui.slots.SlotCraftMatrix;
import forestry.core.platform.gui.slots.SlotFiltered;
import forestry.core.platform.gui.slots.SlotOutput;
import forestry.core.platform.inventory.InventoryGhostCrafting;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.tile.TileUtil;
import forestry.core.content.machines.features.FactoryMenuTypes;
import forestry.core.content.machines.inventory.InventoryFabricator;
import forestry.core.content.machines.tiles.TileFabricator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class ContainerFabricator extends ContainerLiquidTanks<TileFabricator> implements IContainerCrafting {
	private final List<ContainerListener> trackedListeners = new ArrayList<>();
	private int previousHeatValue = 0;

	@Override
	public void addSlotListener(ContainerListener listener) {
		super.addSlotListener(listener);
		if (!this.trackedListeners.contains(listener)) {
			this.trackedListeners.add(listener);
		}
	}

	@Override
	public void removeSlotListener(ContainerListener listener) {
		super.removeSlotListener(listener);
		this.trackedListeners.remove(listener);
	}

	public static ContainerFabricator fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileFabricator tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileFabricator.class);
		return new ContainerFabricator(windowId, inv, tile);
	}

	public ContainerFabricator(int windowId, Inventory playerInventory, TileFabricator tile) {
		super(windowId, FactoryMenuTypes.FABRICATOR.menuType(), playerInventory, tile, 8, 129);
		addDataSlots(new SimpleContainerData(4));

		// Internal inventory
		for (int i = 0; i < 2; i++) {
			for (int k = 0; k < 9; k++) {
				this.addSlot(new Slot(this.tile, InventoryFabricator.SLOT_INVENTORY_1 + k + i * 9, 8 + k * 18, 84 + i * 18));
			}
		}

		// Molten resource
		this.addSlot(new SlotFiltered(this.tile, InventoryFabricator.SLOT_METAL, 26, 21));

		// Plan
		this.addSlot(new SlotFiltered(this.tile, InventoryFabricator.SLOT_PLAN, 139, 17));

		// Result
		this.addSlot(new SlotOutput(this.tile, InventoryFabricator.SLOT_RESULT, 139, 53));

		// Crafting matrix
		for (int l = 0; l < 3; l++) {
			for (int k = 0; k < 3; k++) {
				this.addSlot(new SlotCraftMatrix(this, this.tile.getCraftingInventory(), InventoryGhostCrafting.SLOT_CRAFTING_1 + k + l * 3, 67 + k * 18, 17 + l * 18));
			}
		}
	}

	@Override
	public void onCraftMatrixChanged(Container iinventory, int slot) {
	}

	@Override
	public void setData(int messageId, int data) {
		super.setData(messageId, data);

        this.tile.getGUINetworkData(messageId, data);
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		for (ContainerListener crafter : this.trackedListeners) {
            this.tile.sendGUINetworkData(this, crafter);
		}

		int currentHeat = this.tile.getHeat();
		if (currentHeat != this.previousHeatValue) {
			this.previousHeatValue = currentHeat;
			PacketGuiStream packet = new PacketGuiStream(this.containerId, this.tile);
			sendPacketToListeners(packet);
		}
	}
}
