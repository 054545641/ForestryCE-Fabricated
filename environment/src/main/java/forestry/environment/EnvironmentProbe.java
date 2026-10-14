package forestry.environment;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(EnvironmentProbe.ID)
public final class EnvironmentProbe {
    public static final String ID = "forestry_environment";
    private static final DeferredRegister<Consumer<GameTestHelper>> TESTS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, ID);

    static {
        TESTS.register("n5_completion", () -> N5CompletionProbe::verify);
        TESTS.register("n5_block_removal", () -> N5BlockProbe::removal);
        TESTS.register("n5_leaf_lifecycle", () -> N5BlockProbe::leaves);
        TESTS.register("n5_block_hooks", () -> N5BlockProbe::hooks);
        TESTS.register("n5_bee_entities", () -> N5BeeEffectProbe::entities);
        TESTS.register("n5_loot", () -> N5LootProbe::loot);
        TESTS.register("n5_boats", () -> N5BoatProbe::boats);
        TESTS.register("n3_mail_delivery", () -> N3MailProbe::delivery);
        TESTS.register("n3_mail_trade", () -> N3MailProbe::trade);
        TESTS.register("n3_mail_persistence", () -> N3MailProbe::persistence);
        TESTS.register("n3_items", () -> N3ItemProbe::items);
        TESTS.register("n3_item_persistence", () -> N3ItemProbe::persistence);
        TESTS.register("n3_tanks", () -> N3TankProbe::tanks);
        TESTS.register("n3_fluid_machines", () -> N3TankProbe::machines);
        TESTS.register("n3_energy", () -> N3EnergyProbe::energy);
        TESTS.register("n3_fluid_containers", () -> N3FluidProbe::containers);
        TESTS.register("n2_inventory", () -> N2DataProbe::inventory);
        TESTS.register("n2_codecs", () -> N2DataProbe::codecs);
        TESTS.register("n2_saved_data", () -> N2DataProbe::savedData);
        TESTS.register("toolchain", () -> helper -> {
            helper.setBlock(1, 1, 1, net.minecraft.world.level.block.Blocks.STONE);
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, 1, 1, 1);
            helper.succeed();
        });
    }

    public EnvironmentProbe(IEventBus bus) {
        TESTS.register(bus);
        N5BlockProbe.BLOCKS.register(bus);
        N5BlockProbe.TYPES.register(bus);
        N5BlockProbe.ITEMS.register(bus);
        N5CompletionProbe.ITEMS.register(bus);
        N5CompletionProbe.COMPONENTS.register(bus);
        forestry.arboriculture.features.ArboricultureEntities.TYPES.register(bus);
        N5WoodFixtures.BLOCKS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            forestry.arboriculture.entities.ForestryChestBoat.registerCapabilities(event, forestry.arboriculture.features.ArboricultureEntities.CHEST_BOAT.entityType()));
        N3FluidProbe.COMPONENTS.register(bus);
        bus.addListener(EnvironmentProbe::gatherData);
        bus.addListener(N3TankProbe::registerCapabilities);
        LogUtils.getLogger().info("Forestry 26.2 development environment loaded on Java {}", Runtime.version());
    }

    private static void gatherData(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        try {
            event.addProvider(MailAssetDataProbe.provider(java.nio.file.Path.of(System.getProperty("forestry.environment.mailAssetRoot"))));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        event.addProvider(new DataProvider() {
            @Override
            public CompletableFuture<?> run(CachedOutput cache) {
                JsonObject json = new JsonObject();
                json.addProperty("forestry_environment.ready", "Forestry 26.2 development environment");
                return DataProvider.saveStable(cache, json, output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                        .resolve(ID).resolve("lang/en_us.json"));
            }

            @Override
            public String getName() {
                return "Forestry environment probe";
            }
        });
    }
}
