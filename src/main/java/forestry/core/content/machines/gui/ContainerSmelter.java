package forestry.core.content.machines.gui;

import forestry.core.content.machines.features.FactoryMenuTypes;
import forestry.core.content.machines.inventory.InventorySmelter;
import forestry.core.content.machines.tiles.TileSmelter;
import forestry.core.platform.gui.ContainerSocketed;
import forestry.core.platform.gui.slots.SlotFiltered;
import forestry.core.platform.gui.slots.SlotLocked;
import forestry.core.platform.gui.slots.SlotOutput;
import forestry.core.platform.network.packets.PacketItemStackDisplay;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ContainerSmelter extends ContainerSocketed<TileSmelter> {
	private ItemStack oldCraftPreview = ItemStack.EMPTY;

	public static ContainerSmelter fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileSmelter tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileSmelter.class);
		return new ContainerSmelter(windowId, inv, tile);
	}

	public ContainerSmelter(int windowId, Inventory player, TileSmelter tile) {
		super(windowId, FactoryMenuTypes.SMELTER.menuType(), player, tile, 8, 84);

		// Resources
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				this.addSlot(new SlotFiltered(this.tile,
					InventorySmelter.SLOT_INPUT_1 + y + x * 3, // slot index
					21 + x * 18, // x pos
					21 + y * 18)); // y pos
			}
		}

		// Craft Preview display
		this.addSlot(new SlotLocked(this.tile.getCraftPreviewInventory(), 0, 95, 39));

		// Product Inventory
		this.addSlot(new SlotOutput(this.tile, InventorySmelter.SLOT_OUTPUT, 139, 39));
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		Container craftPreviewInventory = this.tile.getCraftPreviewInventory();

		ItemStack newCraftPreview = craftPreviewInventory.getItem(0);
		if (!ItemStack.matches(this.oldCraftPreview, newCraftPreview)) {
			this.oldCraftPreview = newCraftPreview;

			PacketItemStackDisplay packet = new PacketItemStackDisplay(this.tile, newCraftPreview);
			sendPacketToListeners(packet);
		}
	}
}
