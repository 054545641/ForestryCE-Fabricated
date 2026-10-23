package forestry.core.content.machines.tiles;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import forestry.api.core.fluids.FluidUnits;
import java.util.Objects;

import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.machines.fuels.FermenterFuel;
import forestry.api.core.machines.IFermenterRecipe;
import forestry.api.core.machines.IVariableFermentable;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.FilteredTank;
import forestry.core.platform.fluids.FluidHelper;
import forestry.core.platform.fluids.FluidRecipeFilter;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.render.TankRenderInfo;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import forestry.core.platform.util.RecipeUtils;
import forestry.core.content.machines.blocks.BlockFactoryPlain;
import forestry.core.content.machines.features.FactoryTiles;
import forestry.core.content.machines.gui.ContainerFermenter;
import forestry.core.content.machines.inventory.InventoryFermenter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import forestry.api.core.fluids.FluidVolume;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import javax.annotation.Nullable;
import forestry.api.ForestryDataMaps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class TileFermenter extends TilePowered implements WorldlyContainer, ILiquidTankTile {
	private final FilteredTank resourceTank;
	private final FilteredTank productTank;
	private final TankManager tankManager;

	@Nullable
	private IFermenterRecipe currentRecipe;
	private float currentResourceModifier;
	private int fermentationTime = 0;
	private int fermentationTotalTime = 0;
	private int fuelBurnTime = 0;
	private int fuelTotalTime = 0;
	private int fuelCurrentFerment = 0;
	@Nullable
	private net.minecraft.resources.Identifier activeRecipe;
	private record ProcessingState(IFermenterRecipe recipe, net.minecraft.resources.Identifier active, float modifier,
		int time, int total, int fuel, int fuelTotal, int perCycle) {}
	@Override protected Object captureProcessingState() {
		return new ProcessingState(this.currentRecipe, this.activeRecipe, this.currentResourceModifier,
			this.fermentationTime, this.fermentationTotalTime, this.fuelBurnTime, this.fuelTotalTime, this.fuelCurrentFerment);
	}
	@Override protected void restoreProcessingState(Object value) {
		var previous = (ProcessingState) value;
		this.currentRecipe = previous.recipe();
		this.activeRecipe = previous.active();
		this.currentResourceModifier = previous.modifier();
		this.fermentationTime = previous.time();
		this.fermentationTotalTime = previous.total();
		this.fuelBurnTime = previous.fuel();
		this.fuelTotalTime = previous.fuelTotal();
		this.fuelCurrentFerment = previous.perCycle();
	}
	@Override protected CycleResult completeWorkCycle() {
		int before = this.fermentationTime;
		if (workCycle()) return CycleResult.COMPLETE;
		return this.fermentationTime != before ? CycleResult.PROGRESS : CycleResult.BLOCKED;
	}

	public TileFermenter(BlockPos pos, BlockState state) {
		super(FactoryTiles.FERMENTER.tileType(), pos, state, 2000, 80000);
		setEnergyPerWorkCycle(4200);
		setInternalInventory(new InventoryFermenter(this));

		this.resourceTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY, true, false).setFilter(FluidRecipeFilter.FERMENTER_INPUT);
		this.productTank = new FilteredTank(Constants.PROCESSOR_TANK_CAPACITY, false, true).setFilter(FluidRecipeFilter.FERMENTER_OUTPUT);

		// only finished product may leave through automation
		this.tankManager = new TankManager(this, this.resourceTank, this.productTank);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		compoundNBT.putInt("FermentationTime", this.fermentationTime);
		compoundNBT.putInt("FermentationTotalTime", this.fermentationTotalTime);
		compoundNBT.putInt("FuelBurnTime", this.fuelBurnTime);
		compoundNBT.putInt("FuelTotalTime", this.fuelTotalTime);
		compoundNBT.putInt("FuelCurrentFerment", this.fuelCurrentFerment);
		if (this.activeRecipe != null) output.putString("ActiveRecipe", this.activeRecipe.toString());
		output.putFloat("ResourceModifier", this.currentResourceModifier);

        this.tankManager.write(output);

		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.fermentationTime = compoundNBT.getIntOr("FermentationTime", 0);
        this.fermentationTotalTime = compoundNBT.getIntOr("FermentationTotalTime", 0);
        this.fuelBurnTime = compoundNBT.getIntOr("FuelBurnTime", 0);
        this.fuelTotalTime = compoundNBT.getIntOr("FuelTotalTime", 0);
        this.fuelCurrentFerment = compoundNBT.getIntOr("FuelCurrentFerment", 0);
		String active = input.getStringOr("ActiveRecipe", "");
		this.activeRecipe = this.fermentationTime > 0 && !active.isEmpty() ? net.minecraft.resources.Identifier.tryParse(active) : null;
		this.currentResourceModifier = input.getFloatOr("ResourceModifier", 1.0f);
		this.currentRecipe = null;

        this.tankManager.read(compoundNBT, registries);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
        this.tankManager.writeData(data);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
        this.tankManager.readData(data);
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);

		if (updateOnInterval(20)) {
			FluidHelper.drainContainers(this.tankManager, this, InventoryFermenter.SLOT_INPUT);

			FluidVolume fluidStack = this.productTank.getFluid();
			if (fluidStack != null) {
				FluidHelper.fillContainers(this.tankManager, this, InventoryFermenter.SLOT_CAN_INPUT, InventoryFermenter.SLOT_CAN_OUTPUT, fluidStack.getFluid(), true);
			}

			TankRenderInfo resourceTankInfo = this.getResourceTankInfo();
			TankRenderInfo productTankInfo = this.getProductTankInfo();

			int newLevelLeft = resourceTankInfo.getLevel().getLevelScaled(4);
			int newLevelRight = productTankInfo.getLevel().getLevelScaled(4);
			boolean update = false;

			if (state.hasProperty(BlockFactoryPlain.TANK_RESOURCE_LEVEL) &&
				state.getValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL) != newLevelLeft) {
				state = state.setValue(BlockFactoryPlain.TANK_RESOURCE_LEVEL, newLevelLeft);
				update = true;
			}

			if (state.hasProperty(BlockFactoryPlain.TANK_PRODUCT_LEVEL) &&
				state.getValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL) != newLevelRight) {
				state = state.setValue(BlockFactoryPlain.TANK_PRODUCT_LEVEL, newLevelRight);
				update = true;
			}

			if (update) {
				level.setBlock(pos, state, BlockFactoryPlain.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	public boolean workCycle() {
		if (this.currentRecipe == null) {
			return false;
		}

		try (var tx = beginWorkTransaction()) {
			if (this.fermentationTime <= 0) {
				ItemStack resource = getItem(InventoryFermenter.SLOT_RESOURCE);
				if (this.currentRecipe.getInputItem().isPresent()) {
					if (!this.currentRecipe.getInputItem().get().test(resource) || getTransferHandler().getSlot(InventoryFermenter.SLOT_RESOURCE).extract(ItemVariant.of(resource), 1, tx) != 1) return false;
				}
				this.currentResourceModifier = determineResourceMod(resource);
				this.fermentationTotalTime = this.fermentationTime = this.currentRecipe.getFermentationValue();
				this.activeRecipe = this.currentRecipe.getId();
			}
			if (this.fuelBurnTime <= 0) {
				ItemStack fuel = getItem(InventoryFermenter.SLOT_FUEL);
				FermenterFuel definition = ForestryDataMaps.FERMENTER_FUELS.get(fuel.typeHolder());
				if (definition == null || getTransferHandler().getSlot(InventoryFermenter.SLOT_FUEL).extract(ItemVariant.of(fuel), 1, tx) != 1) return false;
				this.fuelBurnTime = this.fuelTotalTime = definition.burnDuration();
				this.fuelCurrentFerment = definition.fermentPerCycle();
			}
			int fermented = Math.min(this.fermentationTime, this.fuelCurrentFerment);
			if (fermented <= 0) return false;
			int productAmount = Math.round(fermented * this.currentRecipe.getModifier() * this.currentResourceModifier);
			FluidVolume product = FluidVolume.milliBuckets(this.currentRecipe.getOutput(), productAmount);
			if (!product.isEmpty() && this.productTank.insertInternal(product.variant(), product.amount(), tx) != product.amount()) return false;
			if (this.resourceTank.extractInternal(this.currentRecipe.getInputFluid().variant(), FluidUnits.fromMilliBuckets(fermented), tx) != FluidUnits.fromMilliBuckets(fermented)) return false;
			this.fuelBurnTime--;
			this.fermentationTime -= fermented;
			boolean complete = this.fermentationTime <= 0;
			if (complete) {
				this.currentRecipe = null;
				this.activeRecipe = null;
			}
			tx.commit();
			return complete;
		}
	}

	private void checkRecipe() {
		if (this.fermentationTime > 0 && this.activeRecipe != null) {
			var saved = RecipeUtils.getRecipe(this.activeRecipe);
			this.currentRecipe = saved != null && saved.value() instanceof IFermenterRecipe recipe ? recipe : null;
			return;
		}

		ItemStack resource = getItem(InventoryFermenter.SLOT_RESOURCE);
		FluidVolume fluid = this.resourceTank.getFluid();

		this.currentRecipe = fluid.isEmpty() ? null : RecipeUtils.getFermenterRecipe(RecipeUtils.getRecipeMap(this.level), resource, fluid);
	}

	private static float determineResourceMod(ItemStack stack) {
		if (stack.getItem() instanceof IVariableFermentable fermentable) {
			return fermentable.getFermentationModifier(stack);
		}
		return 1.0f;
	}

	@Override
	public boolean hasResourcesMin(float percentage) {
		ItemStack fermentationStack = getItem(InventoryFermenter.SLOT_RESOURCE);
		if (fermentationStack.isEmpty()) {
			return false;
		}

		return (float) fermentationStack.getCount() / (float) fermentationStack.getMaxStackSize() > percentage;
	}

	@Override
	public boolean hasFuelMin(float percentage) {
		ItemStack fuelStack = getItem(InventoryFermenter.SLOT_FUEL);
		if (fuelStack.isEmpty()) {
			return false;
		}

		return (float) fuelStack.getCount() / (float) fuelStack.getMaxStackSize() > percentage;
	}

	@Override
	public boolean hasWork() {
		checkRecipe();
		FermenterFuel fuel = ForestryDataMaps.FERMENTER_FUELS.get(getItem(InventoryFermenter.SLOT_FUEL).typeHolder());
		int perCycle = this.fuelBurnTime > 0 ? this.fuelCurrentFerment : fuel == null ? 0 : fuel.fermentPerCycle();
		int remaining = this.fermentationTime > 0 ? this.fermentationTime : this.currentRecipe == null ? 0 : this.currentRecipe.getFermentationValue();
		int fermented = Math.min(remaining, perCycle);

		boolean hasRecipe = this.currentRecipe != null;
		boolean hasFuel = this.fuelBurnTime > 0 || fuel != null;
		boolean hasResource = this.fermentationTime > 0 || hasRecipe && (this.currentRecipe.getInputItem().isEmpty() || this.currentRecipe.getInputItem().get().test(getItem(InventoryFermenter.SLOT_RESOURCE)));
		FluidVolume available = this.resourceTank.getFluid();
		boolean hasFluidVariant = hasRecipe && fermented > 0 && available.getAmount() >= FluidUnits.fromMilliBuckets(fermented)
			&& FluidVolume.isSameFluidSameComponents(available, this.currentRecipe.getInputFluid());
		boolean hasFluidSpace = true;

		if (hasRecipe) {
			configureWork(this.currentRecipe.getId().toString(), 4, 4200);
			float modifier = this.fermentationTime > 0 ? this.currentResourceModifier : determineResourceMod(getItem(InventoryFermenter.SLOT_RESOURCE));
			int productAmount = Math.round(fermented * this.currentRecipe.getModifier() * modifier);
			Fluid output = this.currentRecipe.getOutput();
			FluidVolume fluidStack = FluidVolume.milliBuckets(output, productAmount);
			hasFluidSpace = FluidHelper.simulateFill(this.productTank.internal(), fluidStack) == fluidStack.getAmount();
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(!hasRecipe, ForestryError.NO_RECIPE);
		errorLogic.setCondition(!hasFuel, ForestryError.NO_FUEL);
		errorLogic.setCondition(!hasResource, ForestryError.NO_RESOURCE);
		errorLogic.setCondition(!hasFluidVariant, ForestryError.NO_RESOURCE_LIQUID);
		errorLogic.setCondition(!hasFluidSpace, ForestryError.NO_SPACE_TANK);

		return hasRecipe && hasFuel && hasResource && hasFluidVariant && hasFluidSpace;
	}

	public int getBurnTimeRemainingScaled(int i) {
		if (this.fuelTotalTime == 0) {
			return 0;
		}

		return this.fuelBurnTime * i / this.fuelTotalTime;
	}

	public int getFermentationProgressScaled(int i) {
		if (this.fermentationTotalTime == 0) {
			return 0;
		}

		return this.fermentationTime * i / this.fermentationTotalTime;
	}

	@Override
	public TankRenderInfo getResourceTankInfo() {
		return new TankRenderInfo(this.resourceTank);
	}

	@Override
	public TankRenderInfo getProductTankInfo() {
		return new TankRenderInfo(this.productTank);
	}

	/* SMP GUI */
	public int getGUINetworkData(int index) {
		return switch (index) {
			case 0 -> this.fuelBurnTime;
			case 1 -> this.fuelTotalTime;
			case 2 -> this.fermentationTime;
			case 3 -> this.fermentationTotalTime;
			default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public void getGUINetworkData(int i, int j) {
		switch (i) {
			case 0 -> this.fuelBurnTime = j;
			case 1 -> this.fuelTotalTime = j;
			case 2 -> this.fermentationTime = j;
			case 3 -> this.fermentationTotalTime = j;
		}
	}

	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new ContainerFermenter(windowId, inv, this);
	}

	public SlottedStorage<FluidVariant> getFluidHandler(@Nullable Direction facing) {
		return this.tankManager;
	}
}
