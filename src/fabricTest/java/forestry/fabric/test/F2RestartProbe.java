package forestry.fabric.test;

import forestry.Forestry;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.*;
import java.util.UUID;
import static forestry.fabric.test.F1RegistrationTests.*;

/** Runs only in the explicit two-process persistence profile. The verifier only reads the first JVM's world. */
public final class F2RestartProbe implements ModInitializer {
    private static final BlockPos CHEST = new BlockPos(0, 90, 0);
    private static final com.mojang.authlib.GameProfile PROFILE = new com.mojang.authlib.GameProfile(UUID.fromString("e7a12f56-8797-4e99-8d1f-a77822210002"), "F2Restart");
    private static final SavedDataType<Probe> TYPE = new SavedDataType<>(Identifier.parse("forestry_test:restart"), Probe::new,
        CompoundTag.CODEC.xmap(tag -> new Probe(tag.getLongOr("writer_pid", -1)), probe -> { var tag = new CompoundTag(); tag.putLong("writer_pid", probe.pid); return tag; }), null);
    private static final class Probe extends SavedData {
        private final long pid;
        private Probe() { this(ProcessHandle.current().pid()); }
        private Probe(long pid) { this.pid = pid; }
    }
    @Override public void onInitialize() {
        String phase = System.getProperty("forestry.persistencePhase", "");
        if (!phase.isEmpty()) ServerLifecycleEvents.SERVER_STARTED.register(server -> verify(server, phase));
    }
    private static void verify(MinecraftServer server, String phase) {
        var level = server.overworld();
        var type = SpeciesUtil.BEE_TYPE.get();
        var tracker = type.getBreedingTracker(level, PROFILE);
        level.getChunkAt(CHEST);
        if (phase.equals("seed")) {
            require(tracker.getSpeciesBred() == 0, "Persistence seed world was not fresh");
            tracker.registerSpecies(type.getSpecies(ForestryBeeSpecies.FOREST));
            var stack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id("queen_bee")));
            stack.set(CoreDataComponents.GENOME.get(), type.getSpecies(ForestryBeeSpecies.FOREST).getDefaultGenome());
            stack.set(CoreDataComponents.MATE_GENOME.get(), type.getSpecies(ForestryBeeSpecies.MEADOWS).getDefaultGenome());
            stack.set(CoreDataComponents.BEE_GENERATION.get(), 23);
            level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
            ((ChestBlockEntity) level.getBlockEntity(CHEST)).setItem(0, stack);
            F2RestartContents.seed(level, (ChestBlockEntity) level.getBlockEntity(CHEST));
            var probe = new Probe(); probe.setDirty(); level.getDataStorage().set(TYPE, probe);
            level.getDataStorage().saveAndJoin();
            Forestry.LOGGER.info("FORESTRY_F2_PERSISTENCE_SEEDED pid={}", probe.pid);
        } else {
            var probe = level.getDataStorage().get(TYPE);
            require(probe != null && probe.pid != ProcessHandle.current().pid(), "Verifier did not open a world written by a different JVM");
            require(tracker.getSpeciesBred() == 1 && tracker.isDiscovered(type.getSpecies(ForestryBeeSpecies.FOREST)), "Breeding SavedData did not survive restart");
            require(level.getBlockEntity(CHEST) instanceof ChestBlockEntity, "Seed chest was not saved");
            var stack = ((ChestBlockEntity) level.getBlockEntity(CHEST)).getItem(0);
            require(stack.get(CoreDataComponents.BEE_GENERATION.get()) == 23, "Native chunk item components lost after restart");
            require(stack.get(CoreDataComponents.GENOME.get()) != null && stack.get(CoreDataComponents.MATE_GENOME.get()) != null, "Genome or mate missing after restart");
            F2RestartContents.verify(level, (ChestBlockEntity) level.getBlockEntity(CHEST));
            Forestry.LOGGER.info("FORESTRY_F2_PERSISTENCE_VERIFIED seedPid={} verifierPid={}", probe.pid, ProcessHandle.current().pid());
        }
    }
}
