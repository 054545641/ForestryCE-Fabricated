package forestry.core.platform.tile;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import forestry.api.core.ITransactionalContainer;

import forestry.api.core.IFilterSlotDelegate;
import net.minecraft.world.entity.ContainerUser;
import com.google.common.base.Preconditions;
import forestry.api.core.IErrorLogic;
import forestry.api.core.IErrorLogicSource;
import forestry.api.core.ILocationProvider;
import forestry.api.core.util.TickHelper;
import forestry.core.platform.block.TileStreamUpdateTracker;
import forestry.core.platform.errors.ErrorLogic;
import forestry.core.platform.inventory.FakeInventoryAdapter;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.network.IStreamable;
import forestry.core.platform.util.NBTUtilForestry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import forestry.core.platform.inventory.InventoryTransferView;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public abstract class TileForestry extends DropAwareBlockEntity implements ITransactionalContainer, IStreamable, IErrorLogicSource, WorldlyContainer, IFilterSlotDelegate, ITitled, ILocationProvider, MenuProvider {
	private final ErrorLogic errorHandler = new ErrorLogic();
	private final AdjacentTileCache tileCache = new AdjacentTileCache(this);

	private IInventoryAdapter inventory = FakeInventoryAdapter.INSTANCE;

	// package private for ForestryTicker
	final TickHelper tickHelper;

	public TileForestry(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state);

		this.tickHelper = new TickHelper(pos.hashCode());
	}

	protected AdjacentTileCache getTileCache() {
		return this.tileCache;
	}

	public void onNeighborTileChange(Level world, BlockPos pos, BlockPos neighbor) {
        this.tileCache.onNeighborChange();
	}

	@Override
	public void setRemoved() {
        this.tileCache.purge();
		super.setRemoved();
	}

	@Override
	public void clearRemoved() {
        this.tileCache.purge();
		super.clearRemoved();
	}

	// these are not called automatically, they must be specified in the MachineProperties
	protected void clientTick(Level level, BlockPos pos, BlockState state) {
	}

	protected void serverTick(Level level, BlockPos pos, BlockState state) {
	}

	protected final boolean updateOnInterval(int tickInterval) {
		return this.tickHelper.updateOnInterval(tickInterval);
	}

	// / SAVING & LOADING
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag data = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.inventory.read(data, registries);
	}

	@Deprecated(forRemoval = true)
	public void load(CompoundTag data) {
		ValueIOUtil.readTag(data, getRegistries(), this::loadCustomOnly);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		this.inventory.write(output);
	}

	@Nullable
	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		return NBTUtilForestry.writeStreamableToNbt(this, tag, registries);
	}

	@Override
	public void handleUpdateTag(ValueInput input) {
		CompoundTag tag = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		NBTUtilForestry.readStreamableFromNbt(this, tag, registries);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection connection, ValueInput input) {
		handleUpdateTag(input);
	}

	/* INetworkedEntity */
	protected final void sendNetworkUpdate() {
		TileStreamUpdateTracker.sendVisualUpdate(this);
	}

	/* IStreamable */
	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
	}

	protected HolderLookup.Provider getRegistries() {
		return this.level != null ? this.level.registryAccess() : RegistryAccess.EMPTY;
	}

	@Override
	protected void dropAdditionalContents(ServerLevel level) {
		onDropContents(level);
		if (this instanceof forestry.core.engine.circuits.ISocketable socketable) {
			forestry.core.platform.util.InventoryUtil.dropSockets(socketable, level, getBlockPos());
		}
	}

	// serverside only, called when the block is destroyed and its inventory is spilled into the world
	public void onDropContents(ServerLevel level) {
	}

	// / REDSTONE INFO
	protected boolean isRedstoneActivated() {
		return this.level.getBestNeighborSignal(getBlockPos()) > 0;
	}

	@Override
	public final IErrorLogic getErrorLogic() {
		return this.errorHandler;
	}

	/* NAME */

	/**
	 * Gets the tile's unlocalized name, based on the block at the location of this entity (client-only).
	 */
	@Override
	public Component getTitle() {
		return Component.translatable(getBlockState().getBlock().getDescriptionId());
	}

	/* INVENTORY BASICS */
	@Override
	public ResourceHandler<ItemResource> getTransferHandler() {
		return getInternalInventory().getTransferHandler();
	}

	public IInventoryAdapter getInternalInventory() {
		return this.inventory;
	}

	protected final void setInternalInventory(IInventoryAdapter inv) {
		Preconditions.checkNotNull(inv);
		this.inventory = inv;
		if (inv instanceof forestry.core.platform.inventory.InventoryAdapter adapter) adapter.setInventoryOwner(this::setChanged);
	}

	/* ISidedInventory */

	@Override
	public boolean isEmpty() {
		return getInternalInventory().isEmpty();
	}

	@Override
	public final int getContainerSize() {
		return getInternalInventory().getContainerSize();
	}

	@Override
	public final ItemStack getItem(int slotIndex) {
		return getInternalInventory().getItem(slotIndex);
	}

	@Override
	public ItemStack removeItem(int slotIndex, int amount) {
		return getInternalInventory().removeItem(slotIndex, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slotIndex) {
		return getInternalInventory().removeItemNoUpdate(slotIndex);
	}

	@Override
	public void setItem(int slotIndex, ItemStack itemstack) {
		getInternalInventory().setItem(slotIndex, itemstack);
	}

	@Override
	public final int getMaxStackSize() {
		return getInternalInventory().getMaxStackSize();
	}

	@Override
	public final void startOpen(ContainerUser user) {
		getInternalInventory().startOpen(user);
	}

	@Override
	public final void stopOpen(ContainerUser user) {
		getInternalInventory().stopOpen(user);
	}

	@Override
	public final boolean stillValid(Player player) {
		return getInternalInventory().stillValid(player);
	}

	@Override
	public final boolean canPlaceItem(int slotIndex, ItemStack itemStack) {
		return getInternalInventory().canPlaceItem(slotIndex, itemStack);
	}

	@Override
	public final boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return getInternalInventory().canSlotAccept(slotIndex, stack);
	}

	@Override
	public boolean isLocked(int slotIndex) {
		return getInternalInventory().isLocked(slotIndex);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return getInternalInventory().getSlotsForFace(side);
	}

	@Override
	public final boolean canPlaceItemThroughFace(int slotIndex, ItemStack itemStack, Direction side) {
		return getInternalInventory().canPlaceItemThroughFace(slotIndex, itemStack, side);
	}

	@Override
	public final boolean canTakeItemThroughFace(int slotIndex, ItemStack itemStack, Direction side) {
		return getInternalInventory().canTakeItemThroughFace(slotIndex, itemStack, side);
	}

	@Override
	public void clearContent() {
	}

	@Nullable
	public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction facing) {
		return this.remove ? null : InventoryTransferView.automation(getInternalInventory(), facing);
	}

	@Override
	public Component getDisplayName() {
		return getTitle();
	}
}
