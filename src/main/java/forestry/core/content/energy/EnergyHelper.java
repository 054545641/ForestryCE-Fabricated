package forestry.core.content.energy;

import forestry.core.platform.config.Preference;
import forestry.core.content.energy.tiles.EngineBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

public class EnergyHelper {
	public static ForestryEnergyStorage createStorage(int maxTransfer, int capacity, EnergyTransferMode mode, Runnable changed) {
		return new ForestryEnergyStorage(scaleForDifficulty(maxTransfer), scaleForDifficulty(capacity), mode).onChange(changed);
	}

	public static int scaleForDifficulty(int energyValue) {
		return Math.round(energyValue * Preference.ENERGY_DEMAND_MODIFIER);
	}

	/**
	 * Consumes one work cycle's worth of energy.
	 *
	 * @return true if the energy to do work was consumed
	 */
	public static boolean consumeEnergyToDoWork(ForestryEnergyStorage energyStorage, int stepsPerWorkCycle, int energyPerWorkCycle) {
		if (energyPerWorkCycle == 0) {
			return true;
		}
		int energyPerCycle = (int) Math.ceil(energyPerWorkCycle / (double) Math.max(1, stepsPerWorkCycle));
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			if (energyStorage.extractInternal(energyPerCycle, transaction) != energyPerCycle) return false;
			transaction.commit();
			return true;
		}
	}

	public static int sendEnergy(ForestryEnergyStorage energyStorage, Direction orientation, @Nullable BlockEntity tile) {
		return sendEnergy(energyStorage, orientation, tile, Integer.MAX_VALUE, false);
	}

	public static int sendEnergy(ForestryEnergyStorage energyStorage, Direction face, @Nullable BlockEntity tile, int amount, boolean simulate) {
		if (tile == null || tile.getLevel() == null) {
			return 0;
		}
		EnergyHandler destination = tile instanceof EngineBlockEntity engine
			? engine.getEnergyManager().chainedInput()
			: tile.getLevel().getCapability(Capabilities.Energy.BLOCK, tile.getBlockPos(), face.getOpposite());
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			int moved = EnergyHandlerUtil.move(energyStorage, destination, amount, transaction);
			if (!simulate) {
				transaction.commit();
			}
			return moved;
		}
	}

	public static boolean canSendEnergy(ForestryEnergyStorage energyStorage, Direction orientation, BlockEntity tile) {
		return sendEnergy(energyStorage, orientation, tile, Integer.MAX_VALUE, true) > 0;
	}

	public static boolean isEnergyReceiverOrEngine(Direction side, @Nullable BlockEntity tile) {
		if (tile == null) {
			return false;
		}
		if (tile instanceof EngineBlockEntity) { // engine chaining
			return true;
		}

		if (tile.getLevel() == null) {
			return false;
		}

		EnergyHandler energyStorage = tile.getLevel().getCapability(Capabilities.Energy.BLOCK, tile.getBlockPos(), side);
		if (energyStorage == null) {
			return false;
		}
		try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			return energyStorage.insert(1, transaction) > 0;
		}
	}

}
