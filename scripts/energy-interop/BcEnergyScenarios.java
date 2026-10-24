package energyinterop;

import buildcraft.api.mj.MjRfConversion;
import buildcraft.energy.tile.TileDynamoMJ;
import buildcraft.energy.tile.TileEngineRF;
import buildcraft.lib.BCLibConfig;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.pipe.flow.PipeFlowRedstoneFlux;
import buildcraft.transport.tile.TilePipeHolder;
import forestry.core.content.energy.tiles.EngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import team.reborn.energy.api.EnergyStorage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static energyinterop.EnergyInteropProbe.require;

public final class BcEnergyScenarios {
    private static final long SEED_E = 4000;

    public static List<EnergyInteropProbe.Scenario> create(ServerLevel level) {
        return List.of(new ForestryToBc(level), new BcToForestry(level));
    }

    private static final class ForestryToBc implements EnergyInteropProbe.Scenario {
        private final EngineBlockEntity source;
        private final TileEngineRF receiver;
        private final List<TilePipeHolder> pipes;
        private long peakReceived;
        private long peakPipeEnergy;

        private ForestryToBc(ServerLevel level) {
            source = EnergyInteropProbe.forestryEngine(level, pos(1, 0), Direction.EAST, SEED_E);
            pipes = List.of(pipe(level, pos(2, 0), "wood_rf"), pipe(level, pos(3, 0), "stone_rf"));
            receiver = (TileEngineRF) EnergyInteropProbe.place(level, pos(4, 0), "buildcraftenergy:engine_rf");
            receiver.setOrientation(Direction.EAST);
            receiver.checkRedstoneLevel();
            require(EnergyInteropProbe.storage(level, pos(4, 0), Direction.WEST).supportsInsertion(), "BC Energy Engine lacks its E input");
            require(receiver.getCurrentFe() == 0 && receiver.getPower() == 0, "BC receiver must start empty");
        }

        @Override public String name() { return "forestry_to_bc_rf_pipes_and_energy_engine"; }

        @Override public void sample() {
            peakReceived = Math.max(peakReceived, receiver.getCurrentFe());
            peakPipeEnergy = Math.max(peakPipeEnergy, pipeEnergy(pipes));
            require(source.getEnergyManager().getAmount() + receiver.getCurrentFe() + pipeEnergy(pipes) <= SEED_E,
                "Forestry-to-BC transfer created energy");
        }

        @Override public Map<String, Object> verify() {
            long remaining = source.getEnergyManager().getAmount();
            long received = receiver.getCurrentFe();
            long buffered = pipeEnergy(pipes);
            require(received == SEED_E, "BC Energy Engine did not receive all 4000 E: received=" + received);
            require(remaining == 0 && buffered == 0 && receiver.getPower() == 0,
                "Forestry-to-BC final balance is not exact: source=" + remaining + ", pipes=" + buffered);
            var result = details("Forestry peat engine -> BC wood RF pipe -> BC stone RF pipe -> BC Energy Engine", received, remaining, buffered);
            result.put("peakReceivedE", peakReceived);
            result.put("peakPipeEnergyE", peakPipeEnergy);
            result.put("receiverMicroMj", receiver.getPower());
            result.put("receiverRedstone", false);
            return result;
        }
    }

    private static final class BcToForestry implements EnergyInteropProbe.Scenario {
        private final TileDynamoMJ source;
        private final EnergyStorage receiver;
        private final List<TilePipeHolder> pipes;
        private final long microMjPerE;
        private final long seedMicroMj;
        private long peakReceived;
        private long peakPipeEnergy;

        private BcToForestry(ServerLevel level) {
            source = (TileDynamoMJ) EnergyInteropProbe.place(level, pos(1, 8), "buildcraftenergy:mj_dynamo");
            source.setOrientation(Direction.EAST);
            pipes = List.of(pipe(level, pos(2, 8), "wood_rf"), pipe(level, pos(3, 8), "stone_rf"));
            EnergyInteropProbe.place(level, pos(4, 8), "forestry:centrifuge");
            receiver = EnergyInteropProbe.storage(level, pos(4, 8), Direction.WEST);
            require(receiver.supportsInsertion() && receiver.getAmount() == 0, "Forestry receiver must expose an empty E input");
            microMjPerE = MjRfConversion.createParsed(BCLibConfig.mjRfConversionAmount.get()).mjPerRf;
            require(microMjPerE > 0, "BC MJ/E conversion is disabled");
            seedMicroMj = Math.multiplyExact(SEED_E, microMjPerE);
            require(source.getMjReceiver().receivePower(seedMicroMj, false) == 0, "BC dynamo rejected the finite MJ seed");
            require(source.getMjBattery().getStored() == seedMicroMj && source.getCurrentFe() == 0, "BC source seed mismatch");
            level.setBlockAndUpdate(pos(1, 8).above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
            source.checkRedstoneLevel();
            require(EnergyInteropProbe.storage(level, pos(1, 8), Direction.EAST).supportsExtraction(), "BC dynamo lacks its E output");
        }

        @Override public String name() { return "bc_mj_dynamo_through_rf_pipes_to_forestry"; }

        @Override public void sample() {
            peakReceived = Math.max(peakReceived, receiver.getAmount());
            peakPipeEnergy = Math.max(peakPipeEnergy, pipeEnergy(pipes));
            long electric = source.getCurrentFe() + receiver.getAmount() + pipeEnergy(pipes);
            require(source.getMjBattery().getStored() + electric * microMjPerE <= seedMicroMj,
                "BC-to-Forestry transfer created energy");
        }

        @Override public Map<String, Object> verify() {
            long received = receiver.getAmount();
            long buffered = pipeEnergy(pipes);
            long remainingMicroMj = source.getMjBattery().getStored();
            long remainingE = source.getCurrentFe();
            require(received == SEED_E, "Forestry did not receive all 4000 E from BC: received=" + received);
            require(remainingE == 0 && buffered == 0 && remainingMicroMj == 0,
                "BC-to-Forestry final balance is not exact: sourceE=" + remainingE + ", microMJ=" + remainingMicroMj + ", pipes=" + buffered);
            var result = details("BC MJ Dynamo -> BC wood RF pipe -> BC stone RF pipe -> Forestry centrifuge", received, remainingE, buffered);
            result.put("seedMicroMj", seedMicroMj);
            result.put("sourceRemainingMicroMj", remainingMicroMj);
            result.put("microMjPerE", microMjPerE);
            result.put("peakReceivedE", peakReceived);
            result.put("peakPipeEnergyE", peakPipeEnergy);
            return result;
        }
    }

    private static Map<String, Object> details(String path, long received, long remaining, long buffered) {
        var result = new LinkedHashMap<String, Object>();
        result.put("path", path);
        result.put("seedEquivalentE", SEED_E);
        result.put("receivedE", received);
        result.put("sourceRemainingE", remaining);
        result.put("pipeBufferE", buffered);
        result.put("forestryFePerE", 1);
        result.put("measurement", "normal server ticks; finite setup seed; no manual transfer during observation");
        return result;
    }

    private static TilePipeHolder pipe(ServerLevel level, BlockPos pos, String name) {
        level.setBlockAndUpdate(pos, BCTransportBlocks.PIPE_HOLDER.defaultBlockState());
        var holder = (TilePipeHolder) level.getBlockEntity(pos);
        require(holder != null, "Missing BC pipe holder at " + pos);
        var item = BuiltInRegistries.ITEM.getValue(Identifier.parse("buildcrafttransport:pipe_" + name));
        require(item != null && item != Items.AIR, "Missing BC pipe item " + name);
        holder.onPlacedBy(null, new ItemStack(item));
        holder.wakePipe();
        holder.setChanged();
        require(holder.getPipe().getFlow() instanceof PipeFlowRedstoneFlux, "BC pipe does not carry E: " + name);
        return holder;
    }

    private static long pipeEnergy(List<TilePipeHolder> pipes) {
        long total = 0;
        for (var holder : pipes) {
            var flow = (PipeFlowRedstoneFlux) holder.getPipe().getFlow();
            for (var section : flow.getSections().values()) total += section.getAmountAsLong();
        }
        return total;
    }

    private static BlockPos pos(int x, int z) { return new BlockPos(x, 100, z); }
}
