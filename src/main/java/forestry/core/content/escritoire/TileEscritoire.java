package forestry.core.content.escritoire;

import com.mojang.authlib.GameProfile;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.features.CoreTiles;
import forestry.core.platform.inventory.InventoryAnalyzer;
import forestry.core.platform.inventory.InventoryEscritoire;
import forestry.core.platform.inventory.watchers.ISlotPickupWatcher;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.network.packets.PacketItemStackDisplay;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import forestry.core.platform.tile.IItemStackDisplay;
import forestry.core.platform.tile.TileBase;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileEscritoire extends TileBase implements WorldlyContainer, ISlotPickupWatcher, IStreamableGui, IItemStackDisplay {
	private final EscritoireGame game = new EscritoireGame();
	private ItemStack individualOnDisplayClient = ItemStack.EMPTY;

	public TileEscritoire(BlockPos pos, BlockState state) {
		super(CoreTiles.ESCRITOIRE.tileType(), pos, state);
		setInternalInventory(new InventoryEscritoire(this));
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.game.read(compoundNBT, registries);
	}


	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.game.write(nbt, registries);
	
		ValueIOUtil.mergeInto(output, nbt);
	}

	/* GAME */
	public EscritoireGame getGame() {
		return this.game;
	}

	public void choose(GameProfile gameProfile, int index) {
        this.game.choose(index);
		processTurnResult(gameProfile);
	}

	private void processTurnResult(GameProfile gameProfile) {
		if (getGame().getStatus() != EscritoireGame.Status.SUCCESS) {
			return;
		}

		IIndividual individual = IIndividualHandlerItem.getIndividual(getItem(InventoryEscritoire.SLOT_ANALYZE));
		if (individual == null) {
			return;
		}

		ISpecies<?> species = individual.getSpecies();
		ISpeciesType<?, ?> root = species.getType();
		for (ItemStack itemstack : root.getResearchBounty(species.cast(), this.level, gameProfile, individual.cast(), this.game.getBountyLevel())) {
			InventoryUtil.addStack(getInternalInventory(), itemstack, InventoryEscritoire.SLOT_RESULTS_1, InventoryEscritoire.SLOTS_RESULTS_COUNT, true);
		}
	}

	private boolean areProbeSlotsFilled() {
		int filledSlots = 0;
		int required = this.game.getSampleSize(InventoryEscritoire.SLOTS_INPUT_COUNT);
		for (int i = InventoryEscritoire.SLOT_INPUT_1; i < InventoryEscritoire.SLOT_INPUT_1 + required; i++) {
			if (!getItem(i).isEmpty()) {
				filledSlots++;
			}
		}

		return filledSlots >= required;
	}

	public void probe() {
		if (this.level.isClientSide()) {
			return;
		}

		ItemStack analyze = getItem(InventoryEscritoire.SLOT_ANALYZE);

		if (!analyze.isEmpty() && areProbeSlotsFilled()) {
            this.game.probe(analyze, this, InventoryEscritoire.SLOT_INPUT_1, InventoryEscritoire.SLOTS_INPUT_COUNT);
		}
	}

	/* NETWORK */
	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
        this.game.writeData(data);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
        this.game.readData(data);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
		ItemStack displayStack = getIndividualOnDisplay();
		ItemStack.OPTIONAL_STREAM_CODEC.encode(data, displayStack);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
        this.individualOnDisplayClient = ItemStack.OPTIONAL_STREAM_CODEC.decode(data);
	}

	/* ISlotPickupWatcher */
	@Override
	public void onTake(int slotIndex, Player player) {
		if (slotIndex == InventoryEscritoire.SLOT_ANALYZE) {
            this.game.reset();
			PacketItemStackDisplay packet = new PacketItemStackDisplay(this, getIndividualOnDisplay());
			NetworkUtil.sendNetworkPacket(packet, this.worldPosition, this.level);
		}
	}

	@Override
	public void setItem(int slotIndex, ItemStack itemstack) {
		super.setItem(slotIndex, itemstack);
		if (slotIndex == InventoryEscritoire.SLOT_ANALYZE) {
			if (this.level != null && !this.level.isClientSide()) {
				PacketItemStackDisplay packet = new PacketItemStackDisplay(this, getIndividualOnDisplay());
				NetworkUtil.sendNetworkPacket(packet, this.worldPosition, this.level);
			}
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerEscritoire(windowId, player.getInventory(), this);
	}

	@Override
	public void handleItemStackForDisplay(ItemStack itemStack) {
		if (!ItemStack.matches(itemStack, this.individualOnDisplayClient)) {
            this.individualOnDisplayClient = itemStack;
		}
	}

	public ItemStack getIndividualOnDisplay() {
		if (this.level == null || this.level.isClientSide()) {
			return this.individualOnDisplayClient;
		}
		return getItem(InventoryAnalyzer.SLOT_ANALYZE);
	}
}
