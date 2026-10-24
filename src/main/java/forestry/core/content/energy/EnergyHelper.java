package forestry.core.content.energy;

import forestry.core.platform.config.Preference;
import forestry.core.platform.tile.IChainedPowerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

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
		try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			if (energyStorage.extractInternal(energyPerCycle, transaction) != energyPerCycle) return false;
			transaction.commit();
			return true;
		}
	}

	public static int sendEnergy(ForestryEnergyStorage energyStorage, Direction orientation, @Nullable BlockEntity tile) {
		return sendEnergy(energyStorage, orientation, tile, Integer.MAX_VALUE, false);
	}

	public static int sendEnergy(ForestryEnergyStorage energyStorage, Direction face, @Nullable BlockEntity tile, int amount, boolean simulate) {
		if (tile == null || tile.isRemoved() || tile.getLevel() == null) {
			return 0;
		}
		return sendEnergy(energyStorage, face, tile.getLevel(), tile.getBlockPos(), amount, simulate);
	}

	public static int sendEnergy(ForestryEnergyStorage energyStorage, Direction face, Level level, BlockPos target, int amount, boolean simulate) {
		if (level.isClientSide()) return 0;
		BlockEntity tile = level.getBlockEntity(target);
		EnergyStorage destination = tile instanceof IChainedPowerHandler engine && !tile.isRemoved()
			? engine.getEnergyManager().chainedInput()
			: EnergyStorage.SIDED.find(level, target, face.getOpposite());
		try (var transaction = Transaction.openNested(Transaction.getCurrentUnsafe())) {
			int moved = Math.toIntExact(EnergyStorageUtil.move(energyStorage, destination, amount, transaction));
			if (!simulate) {
				transaction.commit();
			}
			return moved;
		}
	}

	public static boolean canSendEnergy(ForestryEnergyStorage energyStorage, Direction orientation, BlockEntity tile) {
		return sendEnergy(energyStorage, orientation, tile, Integer.MAX_VALUE, true) > 0;
	}

	public static boolean canSendEnergy(ForestryEnergyStorage energyStorage, Direction orientation, Level level, BlockPos target) {
		return sendEnergy(energyStorage, orientation, level, target, Integer.MAX_VALUE, true) > 0;
	}

	public static boolean isEnergyReceiverOrEngine(Direction side, @Nullable BlockEntity tile) {
		if (tile == null || tile.isRemoved() || tile.getLevel() == null) {
			return false;
		}
		return isEnergyReceiverOrEngine(side, tile.getLevel(), tile.getBlockPos());
	}

	public static boolean isEnergyReceiverOrEngine(Direction side, Level level, BlockPos target) {
		BlockEntity tile = level.getBlockEntity(target);
		if (tile instanceof IChainedPowerHandler && !tile.isRemoved()) { // engine chaining
			return true;
		}
		EnergyStorage energyStorage = EnergyStorage.SIDED.find(level, target, side);
		// placement also runs on clients, where even simulated transfers are forbidden
		return energyStorage != null && energyStorage.supportsInsertion();
	}

}
