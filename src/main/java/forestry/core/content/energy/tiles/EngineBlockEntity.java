package forestry.core.content.energy.tiles;

import forestry.api.core.ForestryError;
import forestry.api.core.IErrorLogic;
import forestry.core.platform.config.Constants;
import forestry.core.platform.network.IStreamableGui;
import forestry.core.platform.network.packets.PacketActiveUpdate;
import forestry.core.platform.tile.IActivatable;
import forestry.core.platform.tile.TemperatureState;
import forestry.core.platform.tile.TileBase;
import forestry.core.platform.util.NetworkUtil;
import forestry.core.content.energy.EnergyHelper;
import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import forestry.core.content.energy.blocks.EngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public abstract class EngineBlockEntity extends TileBase implements IActivatable, IStreamableGui {
	private static final int CANT_SEND_ENERGY_TIME = 20;

	private boolean active = false; // Used for smp.
	private int cantSendEnergyCountdown = CANT_SEND_ENERGY_TIME;
	/**
	 * Indicates whether the piston is receding from or approaching the
	 * combustion chamber
	 */
	public int stagePiston = 0;
	/**
	 * Piston speed as supplied by the server
	 */
	public float pistonSpeedServer = 0;

	protected int currentOutput = 0;
	protected int heat;
	protected final int maxHeat;
	protected boolean forceCooldown = false;
	public float progress;
	protected final ForestryEnergyStorage energyStorage;
	private final String hintKey;
	private record BurnState(int heat, int output, Object fuel) {}
	private final SnapshotJournal<BurnState> burnJournal = new SnapshotJournal<>() {
		@Override protected BurnState createSnapshot() { return new BurnState(heat, currentOutput, captureFuelState()); }
		@Override protected void revertToSnapshot(BurnState state) {
			heat = state.heat(); currentOutput = state.output(); restoreFuelState(state.fuel());
		}
		@Override protected void onRootCommit(BurnState previous) {
			setChanged();
			if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	};
	protected Object captureFuelState() { return null; }
	protected void restoreFuelState(Object state) {}

	/** Fuel reservation, burn progress and generated energy share the caller's transaction. */
	public final void burn() {
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			this.burnJournal.updateSnapshots(transaction);
			burnFuel();
			transaction.commit();
		}
	}

	public final void processEngineStep() {
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			this.burnJournal.updateSnapshots(transaction);
			dissipateHeat();
			generateHeat();
			if (mayBurn()) burnFuel();
			else this.energyStorage.drainEnergy(20);
			transaction.commit();
		}
	}

	protected EngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String hintKey, int maxHeat, int maxEnergy) {
		super(type, pos, state);
		this.hintKey = hintKey;
		this.maxHeat = maxHeat;
		this.energyStorage = EnergyHelper.createStorage(2000, maxEnergy, EnergyTransferMode.EXTRACT, this::setChanged);
	}

	public String getHintKey() {
		return this.hintKey;
	}

	protected void addHeat(int i) {
        this.heat += i;

		if (this.heat > this.maxHeat) {
            this.heat = this.maxHeat;
		}
	}

	protected abstract void dissipateHeat();

	protected abstract void generateHeat();

	protected boolean mayBurn() {
		return !this.forceCooldown;
	}

	protected abstract void burnFuel();


	@Override
	public void clientTick(Level level, BlockPos pos, BlockState state) {
		if (this.stagePiston != 0) {
            this.progress += this.pistonSpeedServer;

			if (this.progress > 1) {
                this.stagePiston = 0;
                this.progress = 0;
			}
		} else if (this.active) {
            this.stagePiston = 1;
		}
	}

	@Override
	public void serverTick(Level level, BlockPos pos, BlockState state) {
		TemperatureState energyState = getTemperatureState();
		if (energyState == TemperatureState.MELTING && this.heat > 0) {
            this.forceCooldown = true;
		} else if (this.forceCooldown && this.heat <= 0) {
            this.forceCooldown = false;
		}

		IErrorLogic errorLogic = getErrorLogic();
		errorLogic.setCondition(this.forceCooldown, ForestryError.FORCED_COOLDOWN);

		boolean enabledRedstone = isRedstoneActivated();
		errorLogic.setCondition(!enabledRedstone, ForestryError.NO_REDSTONE);

		// Determine targeted tile
		BlockState blockState = getBlockState();
		Direction facing = blockState.getValue(EngineBlock.VERTICAL_FACING);
		BlockEntity tile = level.getBlockEntity(getBlockPos().relative(facing));

		float newPistonSpeed = getPistonSpeed();
		if (newPistonSpeed != this.pistonSpeedServer) {
            this.pistonSpeedServer = newPistonSpeed;
			sendNetworkUpdate();
		}

		if (this.stagePiston != 0) {
            this.progress += this.pistonSpeedServer;

			EnergyHelper.sendEnergy(this.energyStorage, facing, tile);

			if (this.progress > 0.25 && this.stagePiston == 1) {
                this.stagePiston = 2;
			} else if (this.progress >= 0.5) {
                this.progress = 0;
                this.stagePiston = 0;
			}
		} else if (enabledRedstone && EnergyHelper.isEnergyReceiverOrEngine(facing.getOpposite(), tile)) {
			if (EnergyHelper.canSendEnergy(this.energyStorage, facing, tile)) {
                this.stagePiston = 1; // If we can transfer energy, start running
				setActive(true);
                this.cantSendEnergyCountdown = CANT_SEND_ENERGY_TIME;
			} else {
				if (isActive()) {
                    this.cantSendEnergyCountdown--;
					if (this.cantSendEnergyCountdown <= 0) {
						setActive(false);
					}
				}
			}
		} else {
			setActive(false);
		}

		processEngineStep();
	}

	@Override
	public boolean isActive() {
		return this.active;
	}

	@Override
	public void setActive(boolean active) {
		if (this.active == active) {
			return;
		}
		this.active = active;

		if (!this.level.isClientSide()) {
			NetworkUtil.sendNetworkPacket(new PacketActiveUpdate(this), this.worldPosition, this.level);
		}
	}

	// STATE INFORMATION
	protected double getHeatLevel() {
		return (double) this.heat / (double) this.maxHeat;
	}

	protected abstract boolean isBurning();

	public int getBurnTimeRemainingScaled(int i) {
		return 0;
	}

	public boolean hasFuelMin(float percentage) {
		return false;
	}

	public int getCurrentOutput() {
		if (canOutput()) {
			return this.currentOutput;
		} else {
			return 0;
		}
	}

	/**
	 * Same as {@link #getCurrentOutput()}, but as a decimal amount. Engines that generate a
	 * fractional amount of energy per tick override this so the GUI reads the rate itself instead
	 * of flickering between the two whole numbers the rate rounds to.
	 *
	 * @return RF produced per tick, as a decimal amount
	 */
	public double getCurrentOutputRate() {
		return getCurrentOutput();
	}

	protected boolean canOutput() {
		return isBurning() && isRedstoneActivated();
	}

	public int getHeat() {
		return this.heat;
	}

	/**
	 * Returns the current energy state of the engine
	 */
	public TemperatureState getTemperatureState() {
		return TemperatureState.getState(this.heat, this.maxHeat);
	}

	protected float getPistonSpeed() {
		return switch (getTemperatureState()) {
			case COOL -> 0.03f;
			case WARMED_UP -> 0.04f;
			case OPERATING_TEMPERATURE -> 0.05f;
			case RUNNING_HOT -> 0.06f;
			case OVERHEATING -> 0.07f;
			case MELTING -> Constants.ENGINE_PISTON_SPEED_MAX;
			default -> 0;
		};
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);
        this.energyStorage.read(nbt, registries);

        this.heat = nbt.getIntOr("EngineHeat", 0);

        this.progress = nbt.getFloatOr("EngineProgress", 0.0f);
        this.forceCooldown = nbt.getBooleanOr("ForceCooldown", false);
	}


	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);
        this.energyStorage.write(nbt, registries);

		nbt.putInt("EngineHeat", this.heat);
		nbt.putFloat("EngineProgress", this.progress);
		nbt.putBoolean("ForceCooldown", this.forceCooldown);

		ValueIOUtil.mergeInto(output, nbt);
	}

	/* NETWORK */
	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
		data.writeBoolean(this.active);
		data.writeInt(this.heat);
		data.writeFloat(this.pistonSpeedServer);
        this.energyStorage.writeData(data);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
        this.active = data.readBoolean();
        this.heat = data.readInt();
        this.pistonSpeedServer = data.readFloat();
        this.energyStorage.readData(data);
	}

	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		data.writeInt(this.currentOutput);
		data.writeInt(this.heat);
		data.writeBoolean(this.forceCooldown);
        this.energyStorage.writeData(data);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
        this.currentOutput = data.readInt();
        this.heat = data.readInt();
        this.forceCooldown = data.readBoolean();
        this.energyStorage.readData(data);
	}

	public ForestryEnergyStorage getEnergyManager() {
		return this.energyStorage;
	}

	@Nullable
	public EnergyHandler getEnergyHandler(@Nullable Direction side) {
		if (this.remove || side != getBlockState().getValue(EngineBlock.VERTICAL_FACING)) {
			return null;
		}
		return this.energyStorage;
	}
}
