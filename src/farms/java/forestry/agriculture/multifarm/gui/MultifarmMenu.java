package forestry.agriculture.multifarm.gui;

import forestry.api.ForestryConstants;
import forestry.core.platform.advancements.AdvancementHelper;
import forestry.core.platform.gui.ContainerSocketed;
import forestry.core.platform.gui.IContainerTank;
import forestry.core.platform.gui.slots.SlotFiltered;
import forestry.core.platform.gui.slots.SlotLiquidIn;
import forestry.core.platform.gui.slots.SlotOutput;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.tile.TileUtil;
import forestry.agriculture.features.MultifarmMenuTypes;
import forestry.agriculture.multifarm.multiblock.InventoryFarm;
import forestry.agriculture.multifarm.tiles.TileFarm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import forestry.core.platform.fluids.StandardTank;

public class MultifarmMenu extends ContainerSocketed<TileFarm> implements IContainerTank {
	private static final Identifier OPEN_FARM_UI = ForestryConstants.forestry("feed_the_world");

	public static MultifarmMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf data) {
		TileFarm tile = TileUtil.getTile(inv.player.level(), data.readBlockPos(), TileFarm.class);
		return new MultifarmMenu(windowId, inv, tile);
	}

	public MultifarmMenu(int windowId, Inventory playerInventory, TileFarm data) {
		super(windowId, MultifarmMenuTypes.FARM.menuType(), playerInventory, data, 28, 138);

		Player player = playerInventory.player;
		if (player != null) {
			AdvancementHelper.tryUnlock(player, OPEN_FARM_UI);
		}

		// Resources
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 2; j++) {
				this.addSlot(new SlotFiltered(this.tile, InventoryFarm.CONFIG.resourcesStart + j + i * 2, 123 + j * 18, 22 + i * 18));
			}
		}

		// Germlings
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 2; j++) {
				this.addSlot(new SlotFiltered(this.tile, InventoryFarm.CONFIG.germlingsStart + j + i * 2, 164 + j * 18, 22 + i * 18));
			}
		}

		// Production 1
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				this.addSlot(new SlotOutput(this.tile, InventoryFarm.CONFIG.productionStart + j + i * 2, 123 + j * 18, 86 + i * 18));
			}
		}

		// Production 2
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				this.addSlot(new SlotOutput(this.tile, InventoryFarm.CONFIG.productionStart + 4 + j + i * 2, 164 + j * 18, 86 + i * 18));
			}
		}

		// Fertilizer
		this.addSlot(new SlotFiltered(this.tile, InventoryFarm.CONFIG.fertilizerStart, 63, 95));
		// Can Slot
		this.addSlot(new SlotLiquidIn(this.tile, InventoryFarm.CONFIG.canStart, 15, 95));
	}

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();
		PacketGuiStream packet = new PacketGuiStream(this.containerId, this.tile);
		sendPacketToListeners(packet);
	}

	@Override
	public StandardTank getTank(int slot) {
		return this.tile.getMultiblockLogic().getController().getTankManager().getTank(slot);
	}
}
