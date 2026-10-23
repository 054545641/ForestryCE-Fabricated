package forestry.apiculture.bees;

import forestry.apiculture.features.ApicultureMenuTypes;
import forestry.core.platform.gui.ContainerTile;
import forestry.core.platform.network.packets.PacketGuiStream;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import java.util.Objects;

public class BeeHousingMenu extends ContainerTile<AbstractBeeHousingBlockEntity> {
	private final IGuiBeeHousingDelegate delegate;
	private final BeeHousingIcon icon;

	public static BeeHousingMenu fromNetwork(int windowId, Inventory inv, FriendlyByteBuf buffer) {
		AbstractBeeHousingBlockEntity tile = TileUtil.getTile(inv.player.level(), buffer.readBlockPos(), AbstractBeeHousingBlockEntity.class);
		boolean hasFrames = tile instanceof forestry.apiculture.apiary.ApiaryBlockEntity;
		BeeHousingIcon icon = hasFrames ? BeeHousingIcon.APIARY : BeeHousingIcon.BEE_HOUSE;
		return new BeeHousingMenu(windowId, inv, Objects.requireNonNull(tile), hasFrames, icon);
	}

	public BeeHousingMenu(int windowId, Inventory playerInv, AbstractBeeHousingBlockEntity tile, boolean hasFrames, BeeHousingIcon icon) {
		super(windowId, ApicultureMenuTypes.BEE_HOUSING.menuType(), playerInv, tile, 8, 108);
		ContainerBeeHelper.addSlots(this, tile, hasFrames);

		tile.getBeekeepingLogic().clearCachedValues();

		this.delegate = tile;
		this.icon = icon;
	}

	private int beeProgress = -1;

	@Override
	public void broadcastChanges() {
		super.broadcastChanges();

		int beeProgress = this.tile.getBeekeepingLogic().getBeeProgressPercent();
		if (this.beeProgress != beeProgress) {
			this.beeProgress = beeProgress;
			PacketGuiStream packet = new PacketGuiStream(this.containerId, this.tile);
			sendPacketToListeners(packet);
		}
	}

	public IGuiBeeHousingDelegate getDelegate() {
		return this.delegate;
	}

	public BeeHousingIcon getIcon() {
		return this.icon;
	}
}
