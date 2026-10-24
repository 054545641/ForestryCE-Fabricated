package energyinterop;

import forestry.core.content.energy.tiles.EngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import techreborn.blockentity.cable.CableBlockEntity;
import techreborn.blockentity.storage.energy.EnergyStorageBlockEntity;
import team.reborn.energy.api.EnergyStorage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import static energyinterop.EnergyInteropProbe.require;

public final class TrEnergyScenarios {
    private static final long SEED = 4096;

    public static List<EnergyInteropProbe.Scenario> create(ServerLevel level) {
        return List.of(forestryToReborn(level), rebornToForestry(level));
    }

    private static EnergyInteropProbe.Scenario forestryToReborn(ServerLevel level) {
        var sourcePos = new BlockPos(1, 100, 16);
        var targetPos = sourcePos.east(3);
        EngineBlockEntity source = EnergyInteropProbe.forestryEngine(level, sourcePos, Direction.EAST, SEED);
        var target = (EnergyStorageBlockEntity) EnergyInteropProbe.place(level, targetPos, "techreborn:low_voltage_su");
        target.setFacing(Direction.EAST);
        var first = cable(level, sourcePos.east());
        var second = cable(level, sourcePos.east(2));
        EnergyStorage destination = EnergyInteropProbe.storage(level, targetPos, Direction.WEST);
        require(destination.supportsInsertion(), "TR storage must accept from its west input face");
        require(!EnergyInteropProbe.storage(level, targetPos, Direction.EAST).supportsInsertion(),
            "TR storage output face must reject input");
        require(destination.getAmount() == 0, "TR target must start empty");
        return new TransferScenario("forestry_to_tr_via_tr_cables", source.getEnergyManager()::getAmount,
            destination::getAmount, first, second, "forestry:peat_engine", "techreborn:low_voltage_su");
    }

    private static EnergyInteropProbe.Scenario rebornToForestry(ServerLevel level) {
        var sourcePos = new BlockPos(1, 100, 24);
        var targetPos = sourcePos.east(3);
        var source = (EnergyStorageBlockEntity) EnergyInteropProbe.place(level, sourcePos, "techreborn:low_voltage_su");
        source.setFacing(Direction.EAST);
        // a finite charge isolates transfer from generator fuel and recipe consumption
        source.setEnergy(SEED);
        source.setChanged();
        EnergyInteropProbe.place(level, targetPos, "forestry:centrifuge");
        var first = cable(level, sourcePos.east());
        var second = cable(level, sourcePos.east(2));
        EnergyStorage destination = EnergyInteropProbe.storage(level, targetPos, Direction.WEST);
        require(EnergyInteropProbe.storage(level, sourcePos, Direction.EAST).supportsExtraction(),
            "TR storage must expose its east output face");
        require(!EnergyInteropProbe.storage(level, sourcePos, Direction.WEST).supportsExtraction(),
            "TR storage input face must reject extraction");
        require(source.getEnergy() == SEED && destination.getAmount() == 0, "TR source seed or Forestry target initial state differs");
        return new TransferScenario("tr_to_forestry_via_tr_cables", source::getEnergy, destination::getAmount,
            first, second, "techreborn:low_voltage_su", "forestry:centrifuge");
    }

    private static CableBlockEntity cable(ServerLevel level, BlockPos pos) {
        return (CableBlockEntity) EnergyInteropProbe.place(level, pos, "techreborn:insulated_copper_cable");
    }

    private static final class TransferScenario implements EnergyInteropProbe.Scenario {
        private final String name;
        private final LongSupplier source;
        private final LongSupplier destination;
        private final CableBlockEntity first;
        private final CableBlockEntity second;
        private final String sourceBlock;
        private final String targetBlock;
        private int samples;
        private long receivedPeak;
        private long cablePeak;
        private long minimumTotal = SEED;
        private long maximumTotal = SEED;

        private TransferScenario(String name, LongSupplier source, LongSupplier destination,
                                 CableBlockEntity first, CableBlockEntity second, String sourceBlock, String targetBlock) {
            this.name = name;
            this.source = source;
            this.destination = destination;
            this.first = first;
            this.second = second;
            this.sourceBlock = sourceBlock;
            this.targetBlock = targetBlock;
        }

        @Override public String name() { return name; }

        @Override public void sample() {
            samples++;
            long received = destination.getAsLong();
            long buffered = first.getEnergy() + second.getEnergy();
            long total = source.getAsLong() + buffered + received;
            receivedPeak = Math.max(receivedPeak, received);
            cablePeak = Math.max(cablePeak, buffered);
            minimumTotal = Math.min(minimumTotal, total);
            maximumTotal = Math.max(maximumTotal, total);
        }

        @Override public Map<String, Object> verify() {
            long remaining = source.getAsLong();
            long received = destination.getAsLong();
            long buffered = first.getEnergy() + second.getEnergy();
            var data = new LinkedHashMap<String, Object>();
            data.put("sourceBlock", sourceBlock);
            data.put("targetBlock", targetBlock);
            data.put("route", "two techreborn:insulated_copper_cable blocks; eastward source output, westward target input");
            data.put("units", "1 FE = 1 E");
            data.put("seed", SEED);
            data.put("sourceRemaining", remaining);
            data.put("received", received);
            data.put("cableBuffer", buffered);
            data.put("receivedPeak", receivedPeak);
            data.put("cableBufferPeak", cablePeak);
            data.put("minimumTotal", minimumTotal);
            data.put("maximumTotal", maximumTotal);
            data.put("samples", samples);
            require(samples > 0 && received > 0 && remaining < SEED, "No natural TR/Forestry transfer: " + data);
            require(remaining >= 0 && buffered >= 0 && received >= 0, "Negative energy after transfer: " + data);
            require(remaining + buffered + received == SEED && minimumTotal == SEED && maximumTotal == SEED,
                "TR cable transfer changed FE/E units or lost/created energy: " + data);
            return data;
        }
    }
}
