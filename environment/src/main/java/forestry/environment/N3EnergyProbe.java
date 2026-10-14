package forestry.environment;

import forestry.core.content.energy.EnergyTransferMode;
import forestry.core.content.energy.ForestryEnergyStorage;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

public final class N3EnergyProbe {
	public static void energy(GameTestHelper helper) {
		AtomicInteger changed = new AtomicInteger();
		var receiver = new ForestryEnergyStorage(100, 1000, EnergyTransferMode.RECEIVE).onChange(changed::incrementAndGet);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(receiver.insert(500, tx) == 100, "Receiver obeys the insertion limit");
			helper.assertTrue(receiver.extract(100, tx) == 0, "Receive-only storage rejects extraction");
		}
		helper.assertTrue(receiver.getEnergyStored() == 0 && changed.get() == 0, "Aborted insert restores energy without dirty notification");
		try (var outer = Transaction.openRoot()) {
			try (var inner = Transaction.open(outer)) {
				receiver.insert(100, inner);
				inner.commit();
			}
		}
		helper.assertTrue(receiver.getEnergyStored() == 0 && changed.get() == 0, "Parent abort undoes a committed child");
		try (var tx = Transaction.openRoot()) {
			receiver.insert(100, tx);
			tx.commit();
		}
		helper.assertTrue(receiver.getEnergyStored() == 100 && changed.get() == 1, "Root commit marks persistent energy changed");

		var engine = new ForestryEnergyStorage(500, 2000, EnergyTransferMode.EXTRACT);
		engine.setEnergyStored(1500);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(engine.insert(100, tx) == 0, "Engine capability is output-only");
			helper.assertTrue(EnergyHandlerUtil.move(engine, receiver, 500, tx) == 100, "Transfer obeys destination limits");
		}
		helper.assertTrue(engine.getEnergyStored() == 1500 && receiver.getEnergyStored() == 100 && changed.get() == 1, "Transfer abort restores both ends");
		helper.assertTrue(EnergyHandlerUtil.move(engine, receiver, 500, null) == 100, "Transfer commits accepted amount");
		helper.assertTrue(engine.getEnergyStored() == 1400 && receiver.getEnergyStored() == 200 && changed.get() == 2, "Transfer conserves energy and notifies the receiver");
		receiver.setEnergyStored(1000);
		helper.assertTrue(EnergyHandlerUtil.move(engine, receiver, 500, null) == 0 && engine.getEnergyStored() == 1400, "Full receiver does not drain the source");

		var chained = new ForestryEnergyStorage(10, 2000, EnergyTransferMode.EXTRACT);
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(EnergyHandlerUtil.move(engine, chained.chainedInput(), 500, tx) == 500, "Engine chaining bypasses the receiver's external input limit");
		}
		helper.assertTrue(engine.getEnergyStored() == 1400 && chained.getEnergyStored() == 0, "Chaining rollback restores both engines");
		helper.assertTrue(EnergyHandlerUtil.move(engine, chained.chainedInput(), 500, null) == 500, "Engine chaining commits");
		helper.assertTrue(engine.getEnergyStored() == 900 && chained.getEnergyStored() == 500, "Chaining conserves energy");

		var registries = helper.getLevel().registryAccess();
		CompoundTag saved = chained.write(new CompoundTag(), registries);
		helper.assertTrue(saved.getIntOr("Energy", -1) == 500, "Legacy Energy field is retained");
		var restored = new ForestryEnergyStorage(10, 2000, EnergyTransferMode.EXTRACT);
		restored.read(saved, registries);
		helper.assertTrue(restored.getEnergyStored() == 500, "Energy survives NBT round trip");
		restored.generateEnergy(Integer.MAX_VALUE);
		helper.assertTrue(restored.getEnergyStored() == 2000, "Generation clamps without integer overflow");
		restored.read(new CompoundTag(), registries);
		helper.assertTrue(restored.getEnergyStored() == 0, "Missing Energy field clears old state");
		helper.succeed();
	}
}
