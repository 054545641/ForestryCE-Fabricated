package forestry.apiculture.alveary.multiblock;

import forestry.core.platform.util.InventoryData;
import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.apiculture.alveary.AlvearySwarmerMenu;
import forestry.apiculture.hives.Hive;
import forestry.apiculture.hives.HiveDecorator;
import forestry.apiculture.hives.HiveDefinitionSwarmer;
import forestry.apiculture.alveary.AlvearySwarmerInventory;
import forestry.api.core.IInventoryAdapter;
import forestry.core.platform.tile.IActivatable;
import forestry.core.platform.tile.TileUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class AlvearySwarmerBlockEntity extends AbstractAlvearyBlockEntity implements WorldlyContainer, IActivatable, IAlvearyComponent.Active<AlvearyMultiblockLogic>, IAlvearyComponent.HasInventory {
	private final AlvearySwarmerInventory inventory;
	private final ArrayDeque<ItemStack> pendingSpawns = new ArrayDeque<>();

	public AlvearySwarmerBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.SWARMER, pos, state);
		this.inventory = new AlvearySwarmerInventory(this);
	}

	@Override
	public IInventoryAdapter getInternalInventory() {
		return this.inventory;
	}

	@Override
	public boolean allowsAutomation() {
		return true;
	}

	/* UPDATING */
	@Override
	public void updateServer(int tickCount) {
		if (!this.pendingSpawns.isEmpty()) {
			setActive(true);
			if (tickCount % 300 == 0) {
				trySpawnSwarm();
			}
		} else {
			setActive(false);
		}

		if (tickCount % 500 != 0) {
			return;
		}

		ItemStack princessStack = getPrincessStack();
		if (princessStack == null) {
			return;
		}

		float chance = consumeInducerAndGetChance();
		if (chance == 0) {
			return;
		}

		// Try to spawn princess
		if (this.level.getRandom().nextFloat() < chance) {
			// Queue swarm spawn
			IIndividualHandlerItem.ifPresent(princessStack, individual -> {
				if (individual instanceof IBee princess) {
					// setting pristine for the new copy is a pain in the ass so do this instead
					princess.setPristine(false);
					this.pendingSpawns.push(princess.createStack(BeeLifeStage.PRINCESS));
					princess.setPristine(true);
				}
			});
		}
	}

	@Override
	public void updateClient(int tickCount) {
	}

	@Nullable
	private ItemStack getPrincessStack() {
		ItemStack princessStack = getMultiblockLogic().getController().getBeeInventory().getQueen();

		if (SpeciesUtil.BEE_TYPE.get().isMated(princessStack)) {
			return princessStack;
		}

		return null;
	}

	private float consumeInducerAndGetChance() {
		for (int slotIndex = 0; slotIndex < getContainerSize(); slotIndex++) {
			ItemStack stack = getItem(slotIndex);
			float chance = IForestryApi.INSTANCE.getHiveManager().getSwarmingMaterialChance(stack.getItem());
			if (chance != 0.0f) {
				removeItem(slotIndex, 1);
				return chance;
			}
		}

		return 0f;
	}

	private void trySpawnSwarm() {
		ItemStack toSpawn = this.pendingSpawns.peek();
		HiveDefinitionSwarmer hiveDescription = new HiveDefinitionSwarmer(toSpawn);
		Hive hive = new Hive(hiveDescription, HiveDefinitionSwarmer.SWARMER_GEN_CHANCE, List.of());

		ServerLevel level = (ServerLevel) this.level;

		int x = getBlockPos().getX() + level.getRandom().nextInt(40 * 2) - 40;
		int z = getBlockPos().getZ() + level.getRandom().nextInt(40 * 2) - 40;

		if (HiveDecorator.tryGenHive(level, level.getRandom(), x, z, hive)) {
            this.pendingSpawns.pop();
		}
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

		InventoryData.replaceStacks(this.pendingSpawns, compoundNBT.getListOrEmpty("PendingSpawns"), registries);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		InventoryData.writeStacks(this.pendingSpawns, "PendingSpawns", output);
	}

	@Override
	public boolean isActive() {
		return getBlockState().getValue(AlvearyBlock.STATE) == AlvearyBlock.State.ON;
	}

	@Override
	public void setActive(boolean active) {
		if (isActive() != active) {
			this.level.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(AlvearyBlock.STATE, active ? AlvearyBlock.State.ON : AlvearyBlock.State.OFF));
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new AlvearySwarmerMenu(windowId, inv, this);
	}
}
