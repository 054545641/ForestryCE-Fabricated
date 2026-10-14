package forestry.core.content.machines.inventory;

import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.item.ItemResource;

import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.ForestryFluids;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.inventory.InventoryAdapterTile;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.tiles.TileSqueezer;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;

public class InventorySqueezer extends InventoryAdapterTile<TileSqueezer> {
	public static final short SLOT_RESOURCE_1 = 0;
	public static final short SLOTS_RESOURCE_COUNT = 9;
	public static final short SLOT_REMNANT = 9;
	public static final short SLOT_REMNANT_COUNT = 1;
	public static final short SLOT_CAN_INPUT = 10;
	public static final short SLOT_CAN_OUTPUT = 11;

	public InventorySqueezer(TileSqueezer squeezer) {
		super(squeezer, 12, "Items");
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		if (slotIndex == SLOT_CAN_INPUT) {
			// todo only accept a glass bottle when the tank holds a fluid that fillContainers can actually bottle.
			//  Right now a hopper can fill this slot with bottles while the squeezer is making, say, seed oil, and
			//  the slot jams forever with no error shown.
			return FluidHelper.isFillableEmptyContainer(stack) || stack.is(Items.GLASS_BOTTLE);
		}

		if (slotIndex >= SLOT_RESOURCE_1 && slotIndex < SLOT_RESOURCE_1 + SLOTS_RESOURCE_COUNT) {
			if (FluidHelper.isFillableEmptyContainer(stack)) {
				return false;
			}

			RecipeMap recipeManager = RecipeUtils.getRecipeMap(this.tile.getLevel());
			return RecipeUtils.isSqueezerIngredient(recipeManager, stack) || RecipeUtils.getSqueezerContainerRecipe(recipeManager, stack) != null;
		}

		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slotIndex, ItemStack itemstack, Direction side) {
		return slotIndex == SLOT_REMNANT || slotIndex == SLOT_CAN_OUTPUT;
	}

	public boolean hasResources() {
		return !InventoryUtil.isEmpty(this, SLOT_RESOURCE_1, SLOTS_RESOURCE_COUNT);
	}

	public List<ItemStack> getResources() {
		return InventoryUtil.getStacks(this, SLOT_RESOURCE_1, SLOTS_RESOURCE_COUNT);
	}

	public boolean removeResources(List<Ingredient> stacks) {
		Container inventory = new InventoryMapper(this, SLOT_RESOURCE_1, SLOTS_RESOURCE_COUNT);
		return InventoryUtil.consumeIngredients(inventory, stacks, null, false, false, true);
	}

	public boolean addRemnant(ItemStack remnant, boolean doAdd) {
		return InventoryUtil.tryAddStack(this, remnant, SLOT_REMNANT, SLOT_REMNANT_COUNT, true, doAdd);
	}

	public void fillContainers(FluidStack fluidStack, TankManager tankManager) {
		if (getItem(SLOT_CAN_INPUT).isEmpty()) {
			return;
		}
		//dirty dirty dirty!
		if (FluidHelper.isFillableEmptyContainer(getItem(SLOT_CAN_INPUT)))
			FluidHelper.fillContainers(tankManager, this, SLOT_CAN_INPUT, SLOT_CAN_OUTPUT, fluidStack.getFluid(), true);
		else {
			//I'm so sorry I literally don't know any way to make this better.
			//fillContainers(fluidHandler, inv, inputSlot, outputSlot, fluidToFill, getEmptyContainer(inv.getItem(inputSlot)), doFill);
			ItemStack input = this.getItem(SLOT_CAN_INPUT);
			if (input.isEmpty() || (!input.is(Items.GLASS_BOTTLE))) return;

			//First, check what the fluid is to determine the resultant item
			ItemStack result = null;
			int fluidAmount = 0;

			//GROSS DIRTY HARD CODED BLEHHHHH
			// Deviation from 1.20.1: ForestryFluids.is(FluidStack) replaces getFluid().isSame(...).
			if (ForestryFluids.EXPERIENCE.is(fluidStack)) {
				result = new ItemStack(Items.EXPERIENCE_BOTTLE);
				fluidAmount = 250;
			} else if (ForestryFluids.HONEY.is(fluidStack)) {
				result = new ItemStack(Items.HONEY_BOTTLE);
				fluidAmount = 200; //This is because of the recipe for Bottled Honey taking 2 honeydrops which equate to 100mb each. It's... odd.
			} else if (fluidStack.getFluid().isSame(Fluids.WATER)) {
				// Deviation from 1.20.1: PotionUtils is gone in 1.21.1; potions are built from a PotionContents data component.
				result = PotionContents.createItemStack(Items.POTION, Potions.WATER);
				fluidAmount = 250;
			}

			//Apparently if you put an invalid container in, it doesn't give an error
			//Tested by putting a wax capsule in with lava
			if (result == null) return;

			//Then, check if there is actually enough fluid to make this item.
			if (fluidStack.getAmount() < fluidAmount) return;

			try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				var items = getTransferHandler();
				if (items.extract(SLOT_CAN_INPUT, ItemResource.of(input), 1, tx) != 1) return;
				if (tankManager.extract(FluidResource.of(fluidStack), fluidAmount, tx) != fluidAmount) return;
				if (items.insert(SLOT_CAN_OUTPUT, ItemResource.of(result), 1, tx) != 1) return;
				new RootCommitJournal(this::setChanged).updateSnapshots(tx);
				tx.commit();
			}
		}
	}
}
