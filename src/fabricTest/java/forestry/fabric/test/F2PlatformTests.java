package forestry.fabric.test;

import com.google.gson.*;
import forestry.api.*;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.core.genetics.*;
import forestry.core.engine.genetics.BreedingTracker;
import forestry.core.features.CoreDataComponents;
import forestry.core.platform.config.ForestryConfig;
import forestry.core.platform.data.*;
import forestry.core.platform.network.packets.PacketPlatformSnapshot;
import forestry.core.platform.util.SpeciesUtil;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.resource.v1.DataResourceStore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import java.util.*;
import static forestry.fabric.test.F1RegistrationTests.*;

public final class F2PlatformTests {
    @GameTest public void nestedServerReadsKeepOneSnapshot(GameTestHelper test) {
        var value = new LogicalData<>("initial");
        value.set("before");
        LogicalData.withServerSnapshot(() -> {
            require(value.get().equals("before"), "Server snapshot did not capture published data");
            value.set("after");
            require(value.get().equals("before"), "Server read mixed snapshots after publication");
            LogicalData.withServerSnapshot(() -> {
                require(value.get().equals("before"), "Nested server read acquired a different snapshot");
                return null;
            });
            return null;
        });
        require(value.get().equals("after"), "Server read was not released");
        test.succeed();
    }
    private static DataMapLoader.Document document(String json) { return new DataMapLoader.Document("regression pack", JsonParser.parseString(json).getAsJsonObject()); }
    @GameTest public void originalDomainDataIsLive(GameTestHelper test) {
        var definitions = ((DataResourceStore) test.getLevel().getServer()).getOrThrow(GeneticDataLoader.KEY);
        var bees = SpeciesUtil.BEE_TYPE.get();
        require(bees.getSpeciesCount() == definitions.bees().size() && bees.getSpeciesCount() >= 60, "Built-in bee species were dropped");
        require(bees.getSpecies(ForestryBeeSpecies.FOREST).getProducts().stream().anyMatch(product -> product.item() == BuiltInRegistries.ITEM.getValue(id("honey_comb"))), "Real bee product missing");
        require(!bees.getMutations().getMutationsFrom(bees.getSpecies(ForestryBeeSpecies.FOREST)).isEmpty(), "Mutation recipes were not installed");
        require(ForestryRegistries.MUTATION_CONDITION_TYPE.size() == 7 && ForestryRegistries.FLOWER_TYPE_SERIALIZER.size() == 3, "Domain serializers missing");
        int components = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("forestrymail") ? 12 : 11;
        require(BuiltInRegistries.DATA_COMPONENT_TYPE.keySet().stream().filter(key -> key.getNamespace().equals("forestry")).count() == components, "Complete component set missing");
        require(IForestryApi.INSTANCE.getFlowerTypeManager().getAllFlowerTypes().size() >= 15, "Flower definitions missing");
        test.succeed();
    }
    @GameTest public void genomeAndMateSurviveNativeItemPersistence(GameTestHelper test) {
        var bees = SpeciesUtil.BEE_TYPE.get();
        var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id("queen_bee")));
        stack.set(CoreDataComponents.GENOME.get(), bees.getSpecies(ForestryBeeSpecies.FOREST).getDefaultGenome());
        stack.set(CoreDataComponents.MATE_GENOME.get(), bees.getSpecies(ForestryBeeSpecies.MEADOWS).getDefaultGenome());
        stack.set(CoreDataComponents.BEE_PRISTINE.get(), false);
        stack.set(CoreDataComponents.BEE_GENERATION.get(), 7);
        stack.set(CoreDataComponents.HEALTH.get(), 31);
        var ops = test.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var saved = ItemStack.CODEC.encodeStart(ops, stack).getOrThrow();
        var loaded = ItemStack.CODEC.parse(ops, saved).getOrThrow();
        require(ItemStack.CODEC.encodeStart(ops, loaded).getOrThrow().equals(saved), "Genome component NBT changed after read");
        require(loaded.get(CoreDataComponents.MATE_GENOME.get()) != null && loaded.get(CoreDataComponents.BEE_GENERATION.get()) == 7, "Mate/generation lost");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), test.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, loaded);
            var networkCopy = ItemStack.STREAM_CODEC.decode(buffer);
            require(ItemStack.CODEC.encodeStart(ops, networkCopy).getOrThrow().equals(saved), "Genome item network round-trip changed state");
        } finally { buffer.release(); }
        test.succeed();
    }
    @GameTest public void trackerCacheAndMalformedLoadAreAtomic(GameTestHelper test) {
        var type = SpeciesUtil.BEE_TYPE.get();
        var profile = new com.mojang.authlib.GameProfile(UUID.fromString("e7a12f56-8797-4e99-8d1f-a77822210001"), "F2Persistence");
        var tracker = type.getBreedingTracker(test.getLevel(), profile);
        require(tracker == type.getBreedingTracker(test.getLevel(), profile), "SavedDataType cache created duplicate trackers");
        tracker.registerSpecies(type.getSpecies(ForestryBeeSpecies.FOREST));
        test.getLevel().getServer().overworld().getDataStorage().saveAndJoin();
        require(tracker.isDiscovered(type.getSpecies(ForestryBeeSpecies.FOREST)), "Discovered species was not retained");
        var saved = new CompoundTag(); tracker.save(saved);
        var loaded = type.createBreedingTracker(saved);
        require(loaded.isDiscovered(type.getSpecies(ForestryBeeSpecies.FOREST)), "Tracker codec lost species");
        var invalid = saved.copy();
        var added = new ListTag(); added.add(StringTag.valueOf("forestry:meadows")); invalid.put("SD", added);
        invalid.putInt("MD", 2);
        try { loaded.load(invalid); throw new AssertionError("Malformed tracker list accepted"); } catch (IllegalArgumentException expected) { }
        require(!loaded.isDiscovered(type.getSpecies(ForestryBeeSpecies.MEADOWS)), "Failed tracker decode applied partial data");
        test.succeed();
    }
    @GameTest public void dataMapLayersTagsAndFailure(GameTestHelper test) {
        var key = ForestryDataMaps.FARM_FERTILIZERS;
        var low = document("{\"values\":{\"minecraft:oak_planks\":7,\"#minecraft:planks\":3}}");
        var middle = document("{\"values\":{\"minecraft:oak_planks\":9,\"forestry:absent\":{\"value\":6,\"required\":false}}}");
        var high = document("{\"remove\":[\"minecraft:birch_planks\"]}");
        var snapshot = DataMapLoader.parse(Map.of(key, List.of(low, middle, high)), test.getLevel().registryAccess(), 1);
        require(snapshot.get(key, Identifier.parse("minecraft:oak_planks")) == 9, "Higher pack did not win");
        require(snapshot.get(key, Identifier.parse("minecraft:spruce_planks")) == 3, "Tag did not expand");
        require(snapshot.get(key, Identifier.parse("minecraft:birch_planks")) == null, "Removal did not apply");
        var literal = DataMapLoader.parse(Map.of(key, List.of(low)), test.getLevel().registryAccess(), 2);
        require(literal.get(key, Identifier.parse("minecraft:oak_planks")) == 7, "Same-pack tag replaced explicit value");
        try { DataMapLoader.parse(Map.of(key, List.of(document("{\"values\":{\"forestry:absent\":8}}"))), test.getLevel().registryAccess(), 3); throw new AssertionError("Unknown required item accepted"); }
        catch (IllegalArgumentException expected) { }
        require(snapshot.get(key, Identifier.parse("minecraft:oak_planks")) == 9, "Failed parse mutated previous snapshot");
        test.succeed();
    }
    @GameTest public void serverSnapshotRoundTripAndConfigValidation(GameTestHelper test) {
        var server = test.getLevel().getServer();
        var snapshot = ForestryDataMapRuntime.snapshot(server);
        require(snapshot.get(ForestryDataMaps.PEAT_FUELS, id("peat")).powerPerCycle() == 20, "Fuel balance changed");
        require(snapshot.get(ForestryDataMaps.BIOGAS_FUELS, id("biomass")).burnDuration() == 2500, "Biogas map missing");
        var definitions = ((DataResourceStore) server).getOrThrow(GeneticDataLoader.KEY);
        var packet = new PacketPlatformSnapshot(snapshot.revision(), DataMapSync.encode(snapshot, server.registryAccess()), ForestryConfig.serverSnapshot(), definitions.wire());
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), server.registryAccess());
        try {
            PacketPlatformSnapshot.encode(buffer, packet);
            require(buffer.readableBytes() < 1_048_576, "Production snapshot exceeds wire bound");
            var decoded = PacketPlatformSnapshot.decode(buffer);
            require(DataMapSync.decode(decoded.maps(), decoded.revision(), server.registryAccess()).values().equals(snapshot.values()), "DataMap values changed over network");
            require(GeneticDataLoader.decode(decoded.genetics(), server.registryAccess()).bees().size() == definitions.bees().size(), "Domain data lost on network");
        } finally { buffer.release(); }
        var before = ForestryConfig.serverSnapshot(); var invalid = before.deepCopy(); invalid.addProperty("unknown_setting", true);
        try { ForestryConfig.applyServerSnapshot(invalid); throw new AssertionError("Unknown config accepted"); } catch (IllegalArgumentException expected) { }
        require(ForestryConfig.serverSnapshot().equals(before), "Failed config changed local settings");
        test.succeed();
    }
    @GameTest public void geneticProjectionFailureAndPublicationAreAtomic(GameTestHelper test) {
        var server = test.getLevel().getServer();
        var original = ((DataResourceStore) server).getOrThrow(GeneticDataLoader.KEY);
        var type = SpeciesUtil.BEE_TYPE.get();
        var before = type.getSpecies(ForestryBeeSpecies.FOREST);
        var mutations = type.getMutations();
        var farms = forestry.api.IForestryApi.INSTANCE.getFarmingManager();
        var individual = before.createIndividual();
        var invalid = original.wire().deepCopy();
        invalid.getAsJsonObject("bee_species").getAsJsonObject("forestry:forest").addProperty("jubilance", "forestry:missing_f2_jubilance");
        try { GeneticDataLoader.decode(invalid, server.registryAccess()); throw new AssertionError("Unresolvable species was silently skipped"); }
        catch (IllegalArgumentException expected) { }
        require(type.getSpecies(ForestryBeeSpecies.FOREST) == before && type.getMutations() == mutations
            && forestry.api.IForestryApi.INSTANCE.getFarmingManager() == farms, "Failed projection partially published runtime state");
        var badTaxonomy = original.wire().deepCopy();
        var taxon = new com.google.gson.JsonObject(); taxon.addProperty("name", "f2_invalid_taxon"); taxon.addProperty("parent", "f2_missing_parent");
        badTaxonomy.getAsJsonObject("taxon").add("forestry:f2_invalid_taxon", taxon);
        try { GeneticDataLoader.decode(badTaxonomy, server.registryAccess()); throw new AssertionError("Invalid taxonomy silently skipped"); }
        catch (IllegalArgumentException expected) { }
        require(type.getSpecies(ForestryBeeSpecies.FOREST) == before, "Failed taxonomy changed live species");
        var candidate = GeneticDataLoader.decode(original.wire(), server.registryAccess());
        require(type.getSpecies(ForestryBeeSpecies.FOREST) == before, "Preparing a candidate changed the live domain");
        try {
            GeneticDataLoader.apply(candidate);
            require(type.getSpecies(ForestryBeeSpecies.FOREST) != before && individual.getSpecies() == type.getSpecies(ForestryBeeSpecies.FOREST), "Loaded individual retained a stale species after publication");
        } finally { GeneticDataLoader.apply(original); }
        test.succeed();
    }
    @GameTest public void nativeEquipmentLookups(GameTestHelper test) {
        require(ForestryCapabilities.SPECTACLE_VISION.find(new ItemStack(BuiltInRegistries.ITEM.getValue(id("spectacles"))), null) != null, "Spectacles lookup missing");
        require(ForestryCapabilities.BEE_PROTECTION.find(new ItemStack(BuiltInRegistries.ITEM.getValue(id("apiarists_hat"))), null) != null, "Bee protection lookup missing");
        require(ForestryCapabilities.BEE_PROTECTION.find(new ItemStack(Items.LEATHER_HELMET), null) == null, "Unrelated armor gained bee protection");
        test.succeed();
    }
    @GameTest public void legacyConfigMigratesWithBoundsAndLogicalSideIsolation(GameTestHelper test) {
        var builder = new forestry.core.platform.config.ConfigSpec.Builder();
        var pair = builder.configure(definition -> {
            definition.push("fuel"); var amount = definition.defineInRange("amount", 20, 1, 1000); definition.pop(); return amount;
        });
        var spec = pair.getRight(); var amount = pair.getLeft();
        var migrated = spec.convertLegacyToml("# saved NeoForge settings\n[fuel]\namount = 3_00 # per cycle\n");
        require(migrated.get("fuel.amount").getAsInt() == 300, "Legacy table or numeric value changed");
        try { spec.convertLegacyToml("[fuel]\namount = -1"); throw new AssertionError("Invalid legacy value accepted"); }
        catch (IllegalArgumentException expected) { }
        require(amount.get() == 20, "Failed conversion changed live configuration");
        spec.applyRemote(migrated);
        require(amount.get() == 20, "Client configuration override changed the logical server");
        spec.clearRemote();
        test.succeed();
    }
}
