package forestry.core.content.energy.tiles;

import forestry.core.platform.fluids.FluidHelper;
import forestry.api.ForestryDataMaps;
import forestry.api.IForestryApi;
import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.api.core.circuits.ForestryCircuitSocketTypes;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.machines.fuels.BiogasEngineFuel;
import forestry.core.content.energy.features.EnergyTiles;
import forestry.core.content.energy.inventory.InventoryEngineCombustion;
import forestry.core.content.energy.menu.CombustionEngineMenu;
import forestry.core.engine.circuits.IEngineUpgradeable;
import forestry.core.engine.circuits.ISocketable;
import forestry.core.platform.config.Constants;
import forestry.core.platform.fluids.*;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.tile.ILiquidTankTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

import static net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME;

public class CombustionEngineBlockEntity extends EngineBlockEntity implements WorldlyContainer, ILiquidTankTile, IEngineUpgradeable, ISocketable {
	private record FuelState(float burnTime, int coolantTime, float outputBoost, float outputMult, float efficiencyMult, float burnRate) {}
	@Override protected Object captureFuelState() { return new FuelState(this.burnTime, this.coolantTime, this.outputBoost, this.outputMult, this.efficiencyMult, this.burnRate); }
	@Override protected void restoreFuelState(Object value) {
		var state = (FuelState) value;
		this.burnTime = state.burnTime();
		this.coolantTime = state.coolantTime();
		this.outputBoost = state.outputBoost();
		this.outputMult = state.outputMult();
		this.efficiencyMult = state.efficiencyMult();
		this.burnRate = state.burnRate();
	}

	private final StandardTank burnTank;
	private final StandardTank waterTank;
	private final FilteredTank fuelTank;
	private final FilteredTank coolantTank;
	private final TankManager tankManager;
	private float burnTime;
	private int coolantTime;
	//upgrades
	private final InventoryAdapter sockets = new InventoryAdapter(1, "sockets");
	private float outputBoost = 1.0f;
	private float outputMult = 1.0f;
	private float efficiencyMult = 1.0f;
	private float burnRate = 1.0f;

	public CombustionEngineBlockEntity(BlockPos pos, BlockState state) {
		// Deviation from 1.20.1: the hint key was "engine_iron" there, which matches no key in
		// hints.properties. Every other engine uses the dotted form
		super(EnergyTiles.COMBUSTION_ENGINE.tileType(), pos, state, "engine.iron", Constants.ENGINE_COPPER_HEAT_MAX, 80000);

		setInternalInventory(new InventoryEngineCombustion(this));

		this.fuelTank = new FilteredTank(Constants.ENGINE_TANK_CAPACITY).setFilter(ForestryDataMaps.COMBUSTION_FUELS);
		this.coolantTank = new FilteredTank(Constants.ENGINE_TANK_CAPACITY, true, false).setFilter(ForestryDataMaps.COMBUSTION_COOLANTS);
		this.burnTank = StandardTank.variableCapacity(BUCKET_VOLUME);
		this.waterTank = StandardTank.variableCapacity(BUCKET_VOLUME);
		this.tankManager = new TankManager(this, this.fuelTank, this.coolantTank, this.burnTank, this.waterTank);
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		super.serverTick(level, pos, state);
		if (!updateOnInterval(20)) {
			return;
		}

		// Check if we have suitable items waiting in the item slot
		FluidHelper.drainContainers(this.tankManager, this, InventoryEngineCombustion.SLOT_CAN);

		IErrorLogic errorLogic = getErrorLogic();

		errorLogic.setCondition(this.coolantTank.isEmpty() && this.waterTank.isEmpty(), ForestryError.NO_COOLANT);

		errorLogic.setCondition(this.burnTank.isEmpty() && this.fuelTank.isEmpty(), ForestryError.NO_FUEL);
	}

	@Override
	protected void dissipateHeat() {
		if (!isBurning() && this.heat > 0)
			this.heat--;
		int heatToCool = 0;
		double heatLevel = getHeatLevel();
		if (heatLevel > 0.2)
			heatToCool++;
		if (heatLevel > 0.25)
			heatToCool++;
		if (heatLevel > 0.45)
			heatToCool += 2;
		if (heatLevel > 0.55)
			heatToCool += 2;
		if (heatLevel > 0.65)
			heatToCool += 2;
		if (heatLevel > 0.75)
			heatToCool += 2;
		if (heatLevel > 0.85)
			heatToCool += 2;

		FluidStack water = FluidHelper.drain(this.waterTank.internal(), heatToCool);
		this.heat -= water.getAmount();
		if (this.waterTank.isEmpty() && this.coolantTank.getFluidAmount() >= BUCKET_VOLUME) {
			FluidStack fluidStack = FluidHelper.drain(this.coolantTank.internal(), BUCKET_VOLUME);
			if (!fluidStack.isEmpty()) {
				fluidStack.setAmount(determineCoolantTime(fluidStack));
				this.waterTank.setCapacity(fluidStack.getAmount());
				this.waterTank.setFluid(fluidStack);
				if (water.getFluid() != fluidStack.getFluid()) {
					removeEngineUpgrade(0, determineCoolantModifier(water) / 100f, 0);
					applyEngineUpgrade(0, determineCoolantModifier(fluidStack) / 100f, 0);
				}
			}
		}
	}

	@Override
	protected void generateHeat() {
		if (isBurning()) {
			addHeat((int) (2 * this.outputMult));
		}
	}

	@Override
	protected void burnFuel() {

		this.currentOutput = 0;

		if (isRedstoneActivated()) {
			if (this.burnTime > 0 && !this.waterTank.isEmpty()) {
				this.currentOutput = (int) (determineFuelValue(this.burnTank.getFluid()) * this.outputMult);
				if (this.energyStorage.getMaxEnergyStored() - this.energyStorage.getEnergyStored() >= this.currentOutput) {
					this.burnTime -= this.burnRate;
					this.energyStorage.generateEnergy(this.currentOutput);
					FluidStack fuel = this.burnTank.getFluid();
					fuel.setAmount((int) this.burnTime);
					this.burnTank.setFluid(fuel);
					
				} else {
					this.currentOutput = 0;
				}
			} else if (this.burnTime <= 0 && this.fuelTank.getFluidAmount() >= BUCKET_VOLUME) {
				FluidStack fuel = FluidHelper.drain(this.fuelTank.internal(), BUCKET_VOLUME).copy();
				int time = determineBurnTime(fuel);
				if (!fuel.isEmpty()) {
					fuel.setAmount(time);
					this.burnTime = time;
				}
				this.burnTank.setCapacity(time);
				this.burnTank.setFluid(fuel);
			}
		}

	}

	@Override
	protected boolean isBurning() {
		return mayBurn() && (this.level.isClientSide() || (!this.burnTank.isEmpty() && this.currentOutput != 0));
	}

	/**
	 * Returns the fuel value (power per cycle) an item of the passed fluid
	 */
	private static int determineFuelValue(@Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			BiogasEngineFuel fuel = fluidStack.typeHolder().getData(ForestryDataMaps.COMBUSTION_FUELS);
			if (fuel != null) {
				return fuel.powerPerCycle();
			}
		}
		return 0;
	}

	/**
	 * @return Duration of burn cycle of one bucket
	 */
	private static int determineBurnTime(@Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			BiogasEngineFuel fuel = fluidStack.typeHolder().getData(ForestryDataMaps.COMBUSTION_FUELS);
			if (fuel != null) {
				return fuel.burnDuration();
			}
		}
		return 0;
	}

	private static int determineCoolantTime(@Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			BiogasEngineFuel fuel = fluidStack.typeHolder().getData(ForestryDataMaps.COMBUSTION_COOLANTS);
			if (fuel != null) {
				return fuel.burnDuration();
			}
		}
		return 0;
	}

	private static int determineCoolantModifier(@Nullable FluidStack fluidStack) {
		if (fluidStack != null) {
			BiogasEngineFuel fuel = fluidStack.typeHolder().getData(ForestryDataMaps.COMBUSTION_COOLANTS);
			if (fuel != null) {
				return fuel.dissipationMultiplier();
			}
		}
		return 0;
	}

	public boolean isCrushedIce() {
		return this.waterTank.getFluidType() == ForestryFluids.ICE.getFluid();
	}

	// Deviation from 1.20.1: 1.21.1 block entities save through saveAdditional(CompoundTag, HolderLookup.Provider),
	// and the tank manager and the socket inventory take the registry lookup too.
	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
		this.tankManager.write(output);
		nbt.putFloat("burnTime", this.burnTime);
		nbt.putInt("coolantTime", this.coolantTime);
		this.sockets.write(nbt, registries);
	
		ValueIOUtil.mergeInto(output, nbt);
	}

	// Deviation from 1.20.1: load(CompoundTag) became loadAdditional(CompoundTag, HolderLookup.Provider).
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
		this.tankManager.read(nbt, registries);
		this.burnTime = nbt.getFloatOr("burnTime", 0.0f);
		this.coolantTime = nbt.getIntOr("coolantTime", 0);
		this.sockets.read(nbt, registries);

		ItemStack chip = this.sockets.getItem(0);
		if (!chip.isEmpty()) {
			ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(chip);
			if (chipset != null) {
				chipset.onLoad(this);
			}
		}
		if (!this.waterTank.isEmpty())
			applyEngineUpgrade(0, determineCoolantModifier(this.waterTank.getFluid()) / 100f, 0);
	}

	/* NETWORK */
	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
		this.tankManager.writeData(data);
		this.sockets.writeData(data);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
		this.tankManager.readData(data);
		this.sockets.readData(data);
	}

	// Deviation from 1.20.1: the fluid handler is exposed through RegisterCapabilitiesEvent in
	// ModuleEnergy instead of an overridden getCapability.
	@Nullable
	public ResourceHandler<FluidResource> getFluidHandler(@Nullable Direction facing) {
		return this.remove ? null : this.tankManager;
	}

	@Override
	public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
		return new CombustionEngineMenu(windowId, inv, this);
	}

	@Override
	public TankManager getTankManager() {
		return this.tankManager;
	}

	@Override
	public void applyEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {
		//dont stack multiple chokes
		if (!(outputBoost < 0 && this.outputBoost < 0))
			this.efficiencyMult += efficiencyMult;
		this.outputBoost += outputBoost;
		this.outputMult = Math.max(0.5f, this.outputBoost);
		this.burnRate = this.outputMult / Math.min(1.6f, Math.max(this.efficiencyMult, isCrushedIce() ? 1.0f : 0.1f));
	}

	@Override
	public void removeEngineUpgrade(float outputBoost, float efficiencyMult, int heat) {
		//dont unstack multiple chokes
		if (!(outputBoost < 0 && this.outputBoost < outputBoost))
			this.efficiencyMult -= efficiencyMult;
		this.outputBoost -= outputBoost;
		this.outputMult = this.outputBoost;
		this.burnRate = this.outputBoost / Math.max(this.efficiencyMult, isCrushedIce() ? 1.0f : 0.1f);
	}

	@Override
	public int getSocketCount() {
		return this.sockets.getContainerSize();
	}

	@Override
	public ItemStack getSocket(int slot) {
		return this.sockets.getItem(slot);
	}

	@Override
	public void setSocket(int slot, ItemStack stack) {
		if (!stack.isEmpty() && !IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(stack)) {
			return;
		}

		// Dispose correctly of old chipsets
		if (!this.sockets.getItem(slot).isEmpty()) {
			if (IForestryApi.INSTANCE.getCircuitManager().isCircuitBoard(this.sockets.getItem(slot))) {
				ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(this.sockets.getItem(slot));
				if (chipset != null) {
					chipset.onRemoval(this);
				}
			}
		}

		this.sockets.setItem(slot, stack);
		// sockets has no tile link of its own
		setChanged();
		if (stack.isEmpty()) {
			return;
		}

		ICircuitBoard chipset = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
		if (chipset != null) {
			chipset.onInsertion(this);
		}
	}

	@Override
	public Identifier getSocketType() {
		return ForestryCircuitSocketTypes.ENGINE;
	}
}
