package forestry.core.content.sorting.tiles;

import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.filter.FilterData;
import forestry.core.platform.inventory.AdjacentInventoryCache;
import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.ItemStackUtil;
import forestry.core.content.sorting.FilterLogic;
import forestry.core.content.sorting.features.SortingTiles;
import forestry.core.content.sorting.gui.ContainerGeneticFilter;
import forestry.core.content.sorting.inventory.ItemHandlerFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import forestry.core.platform.inventory.InventoryTransferView;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileGeneticFilter extends TileForestry implements IStreamableGui {
	private static final int TRANSFER_DELAY = 5;

	private final FilterLogic logic;
	private final AdjacentInventoryCache inventoryCache;

	public TileGeneticFilter(BlockPos pos, BlockState state) {
		super(SortingTiles.GENETIC_FILTER.tileType(), pos, state);
		this.inventoryCache = new AdjacentInventoryCache(this, getTileCache());
		this.logic = new FilterLogic(this, (logic1, level, player) -> sendToPlayers(level, player));
		setInternalInventory(new InventoryAdapterTile<>(this, 6, "Items"));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag data = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		data.put("Logic", this.logic.write(new CompoundTag(), registries));
	
		ValueIOUtil.mergeInto(output, data);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag data = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.logic.read(data.getCompoundOrEmpty("Logic"), registries);
	}

	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
        this.logic.writeGuiData(data);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
        this.logic.readGuiData(data);
	}

	private void sendToPlayers(ServerLevel server, Player filterChanger) {
		for (Player player : server.players()) {
			if (player != filterChanger && player.containerMenu instanceof ContainerGeneticFilter) {
				if (((ContainerGeneticFilter) filterChanger.containerMenu).hasSameTile((ContainerGeneticFilter) player.containerMenu)) {
					((ContainerGeneticFilter) player.containerMenu).setGuiNeedsUpdate(true);
				}
			}
		}
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		if (updateOnInterval(TRANSFER_DELAY)) {
			for (Direction facing : Direction.values()) {
				ItemStack stack = getItem(facing.get3DDataValue());
				if (stack.isEmpty()) {
					continue;
				}
				ItemStack transferredStack = transferItem(stack, facing);
				int remaining = stack.getCount() - transferredStack.getCount();
				if (remaining > 0) {
					stack = stack.copy();
					stack.setCount(remaining);
					ItemStackUtil.dropItemStackAsEntity(stack.copy(), level, this.worldPosition.getX(), this.worldPosition.getY() + 0.5F, this.worldPosition.getZ());
				}
				setItem(facing.get3DDataValue(), ItemStack.EMPTY);
			}
		}
	}

	public boolean isConnected(Direction facing) {
		if (this.inventoryCache.getAdjacentInventory(facing) != null) {
			return true;
		}
		BlockEntity tileEntity = this.level.getBlockEntity(this.worldPosition.relative(facing));
		return TileUtil.getInventoryFromTile(tileEntity, facing.getOpposite()) != null;
	}

	private ItemStack transferItem(ItemStack itemStack, Direction facing) {
		var target = this.inventoryCache.getAdjacentInventory(facing);
		if (target == null) return ItemStack.EMPTY;
		int slot = facing.get3DDataValue();
		var source = new InventoryTransferView(getTransferHandler(), 1, i -> slot, (i, r) -> false, (i, r) -> true);
		try (var tx = Transaction.openRoot()) {
			int moved = ResourceHandlerUtil.moveStacking(source, target, r -> true, itemStack.getCount(), tx);
			tx.commit();
			return itemStack.copyWithCount(moved);
		}
	}

	public List<Direction> getValidDirections(ItemStack stack, Direction from) {
		IIndividual individual = IIndividualHandlerItem.getIndividual(stack);
		ILifeStage stage = IIndividualHandlerItem.getLifeStage(stack);
		if (individual == null || stage == null) {
			return List.of();
		}

		FilterData filterData = new FilterData(individual, stage);
		List<Direction> validFacings = new ArrayList<>();

		for (Direction facing : Direction.values()) {
			if (facing == from) {
				continue;
			}
			if (isValidFacing(facing, stack, filterData)) {
				validFacings.add(facing);
			}
		}

		return validFacings;
	}

	private boolean isValidFacing(Direction facing, ItemStack itemStack, FilterData filterData) {
		return this.inventoryCache.getAdjacentInventory(facing) != null && this.logic.isValid(facing, itemStack, filterData);
	}

	public FilterLogic getLogic() {
		return this.logic;
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerGeneticFilter(windowId, player.getInventory(), this);
	}

	@Nullable
	public ResourceHandler<ItemResource> getItemHandler(@Nullable Direction facing) {
		if (facing != null) {
			return new ItemHandlerFilter(getTransferHandler(), stack -> getValidDirections(stack, facing));
		}
		return null;
	}
}
