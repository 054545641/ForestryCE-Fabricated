package forestry.core.content.machines.gui;

import forestry.core.platform.gui.ContainerLiquidTanks;
import forestry.core.platform.gui.slots.SlotEmptyLiquidContainerIn;
import forestry.core.platform.gui.slots.SlotFiltered;
import forestry.core.platform.gui.slots.SlotLiquidIn;
import forestry.core.platform.gui.slots.SlotOutput;
import forestry.core.platform.tile.TileUtil;
import forestry.core.content.machines.features.FactoryMenuTypes;
import forestry.core.content.machines.inventory.InventoryFermenter;
import forestry.core.content.machines.tiles.TileFermenter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class ContainerFermenter extends ContainerLiquidTanks<TileFermenter> {
	public static ContainerFermenter fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileFermenter tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileFermenter.class);
		return new ContainerFermenter(windowId, inv, tile);
	}

	public ContainerFermenter(int windowId, Inventory player, TileFermenter tile) {
		super(windowId, FactoryMenuTypes.FERMENTER.menuType(), player, tile, 8, 84);
		addDataSlots(new ContainerData() {
			@Override
			public int get(int index) {
				return tile.getGUINetworkData(index);
			}

			@Override
			public void set(int index, int value) {
				tile.getGUINetworkData(index, value);
			}

			@Override
			public int getCount() {
				return 4;
			}
		});

		this.addSlot(new SlotFiltered(tile, InventoryFermenter.SLOT_RESOURCE, 85, 23));
		this.addSlot(new SlotFiltered(tile, InventoryFermenter.SLOT_FUEL, 75, 57));
		this.addSlot(new SlotOutput(tile, InventoryFermenter.SLOT_CAN_OUTPUT, 150, 58));
		this.addSlot(new SlotEmptyLiquidContainerIn(tile, InventoryFermenter.SLOT_CAN_INPUT, 150, 22));
		this.addSlot(new SlotLiquidIn(tile, InventoryFermenter.SLOT_INPUT, 10, 40));
	}
}
