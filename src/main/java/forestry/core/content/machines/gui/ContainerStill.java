package forestry.core.content.machines.gui;

import forestry.core.platform.gui.ContainerLiquidTanks;
import forestry.core.platform.gui.slots.SlotEmptyLiquidContainerIn;
import forestry.core.platform.gui.slots.SlotLiquidIn;
import forestry.core.platform.gui.slots.SlotOutput;
import forestry.core.platform.tile.TileUtil;
import forestry.core.content.machines.features.FactoryMenuTypes;
import forestry.core.content.machines.inventory.InventoryStill;
import forestry.core.content.machines.tiles.TileStill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class ContainerStill extends ContainerLiquidTanks<TileStill> {
	public static ContainerStill fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileStill tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileStill.class);
		return new ContainerStill(windowId, inv, tile);
	}

	public ContainerStill(int windowId, Inventory player, TileStill tile) {
		super(windowId, FactoryMenuTypes.STILL.menuType(), player, tile, 8, 84);

		this.addSlot(new SlotOutput(tile, InventoryStill.SLOT_PRODUCT, 150, 54));
		this.addSlot(new SlotEmptyLiquidContainerIn(tile, InventoryStill.SLOT_RESOURCE, 150, 18));
		this.addSlot(new SlotLiquidIn(tile, InventoryStill.SLOT_CAN, 10, 36));
	}
}
