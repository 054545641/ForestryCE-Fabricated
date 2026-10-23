package forestry.core.content.backpacks.gui;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.content.backpacks.features.BackpackMenuTypes;
import forestry.core.content.backpacks.inventory.BackpackInventory;
import forestry.core.content.backpacks.items.BackpackItem;
import forestry.core.platform.gui.ContainerItemInventory;
import forestry.core.platform.gui.ContainerNaturalistInventory;
import forestry.core.platform.gui.IGuiSelectable;
import forestry.core.platform.gui.INaturalistMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class ContainerNaturalistBackpack extends ContainerItemInventory<BackpackInventory> implements IGuiSelectable, INaturalistMenu {
	private final SimpleContainerData scrollData = new SimpleContainerData(1);
	private final ISpeciesType<?, ?> speciesRoot;

	public ContainerNaturalistBackpack(int windowId, Inventory inv, BackpackInventory inventory, Identifier rootUid) {
		super(windowId, inventory, inv, 7, 107, BackpackMenuTypes.NATURALIST_BACKPACK.menuType());

		addDataSlots(this.scrollData);
		ContainerNaturalistInventory.addScrollableInventory(this, inventory, this.scrollData);

		this.speciesRoot = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(rootUid);
	}

	public static ContainerNaturalistBackpack makeContainer(int windowId, Player player, ItemStack heldItem, Identifier typeId) {
		BackpackInventory inventory = new BackpackInventory(player, BackpackItem.SLOTS_BACKPACK_APIARIST, heldItem);
		return new ContainerNaturalistBackpack(windowId, player.getInventory(), inventory, typeId);
	}

	@Override
	public void handleSelectionRequest(ServerPlayer player, int primary, int secondary) {
		setScrollRow(primary);
	}

	@Override
	public ISpeciesType<?, ?> getSpeciesType() {
		return this.speciesRoot;
	}

	@Override
	public int getScrollRow() {
		return this.scrollData.get(0);
	}

	@Override
	public void setScrollRow(int row) {
		this.scrollData.set(0, Mth.clamp(row, 0, ContainerNaturalistInventory.MAX_SCROLL));
	}

	public static ContainerNaturalistBackpack fromNetwork(int windowId, Inventory playerInventory, FriendlyByteBuf buffer) {
		ItemStack parent = BackpackItem.readParentIdentity(playerInventory.player, buffer);
        Identifier typeId = buffer.readIdentifier();

		return makeContainer(windowId, playerInventory.player, parent, typeId);
	}
}
