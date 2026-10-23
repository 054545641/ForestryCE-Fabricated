package forestry.fabric.test;

import forestry.Forestry;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.arboriculture.entities.ForestryBoat;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.trees.TreeUtil;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.core.content.machines.tiles.TileCarpenter;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.ArrayList;
import java.util.List;
import static forestry.fabric.test.F1RegistrationTests.*;

@SuppressWarnings("UnstableApiUsage")
public final class F3ClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("fill -8 99 -8 8 99 8 minecraft:stone");
            world.getServer().runCommand("tp @p 0.5 100 0.5 0 12");
            context.runOnClient(client -> {
                var engine = client.particleEngine;
                var destination = new BlockPos(0, 101, 3);
                for (var type : List.of(forestry.apiculture.particles.ApicultureParticles.BEE_EXPLORER_PARTICLE.get(), forestry.apiculture.particles.ApicultureParticles.BEE_ROUND_TRIP_PARTICLE.get()))
                    require(engine.createParticle(new forestry.apiculture.particles.BeeParticleData(type, destination, 0xffee33), 0, 101, 1, 0, 0, 0) != null, "Bee particle provider missing");
                require(engine.createParticle(new forestry.apiculture.particles.BeeTargetParticleData(client.player.getId(), 0xffee33), 0, 101, 1, 0, 0, 0) != null, "Targeted bee particle provider missing");
                require(engine.createParticle(forestry.apiculture.particles.ApicultureParticles.SNOW_PARTICLE.get(), 0, 101, 1, 0, 0, 0) != null, "Snow particle provider missing");
            });
            var menus = new ArrayList<>(List.of("analyzer", "escritoire", "apiarists_chest", "arborists_chest",
                "burn_barrel", "worktable", "genetic_filter", "carpenter", "centrifuge", "thermionic_fabricator", "fermenter", "moistener",
                "smelter", "squeezer", "still", "bottler", "peat_engine", "biogas_engine", "combustion_engine", "solar_engine", "apiary", "bee_house",
                "alveary_block", "alveary_hygroregulator", "alveary_sieve", "alveary_swarmer"));
            if (FabricLoader.getInstance().isModLoaded("forestryfarms")) menus.addAll(List.of("arboretum_managed", "brick_farm_plain"));
            if (FabricLoader.getInstance().isModLoaded("forestrybutterflies")) menus.add("lepidopterists_chest");
            if (FabricLoader.getInstance().isModLoaded("forestrymail")) menus.addAll(List.of("mailbox", "stamp_collector", "trade_station"));
            BlockPos pos = new BlockPos(0, 100, 3);
            for (String name : menus) {
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.closeContainer();
                    var level = server.overworld();
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    var block = BuiltInRegistries.BLOCK.getValue(id(name));
                    require(block != Blocks.AIR, "Missing screen fixture " + name);
                    level.setBlockAndUpdate(pos, block.defaultBlockState());
                });
                context.waitFor(client -> client.level.getBlockState(pos).is(BuiltInRegistries.BLOCK.getValue(id(name)))
                    && client.level.getBlockEntity(pos) != null);
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var entity = server.overworld().getBlockEntity(pos);
                    require(entity instanceof MenuProvider, "No menu provider: " + name);
                    require(player.openMenu((MenuProvider) entity).isPresent(), "Server refused menu: " + name);
                });
                context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?> screen
                    && screen.getMenu() == client.player.containerMenu);
                context.waitTicks(8);
                context.takeScreenshot("forestry-f3-screen-" + name);
                if (name.equals("carpenter") && FabricLoader.getInstance().isModLoaded("jei")) {
                    context.waitFor(client -> F1JeiProbe.isReady());
                    context.runOnClient(F1JeiProbe::transferCarpenter);
                    context.waitTicks(10);
                    world.getServer().runOnServer(server -> require(!((TileCarpenter) server.overworld().getBlockEntity(pos)).getCraftingInventory().isEmpty(),
                        "Real JEI transfer did not reach the server ghost grid"));
                    context.takeScreenshot("forestry-f3-jei-transfer");
                }
                if (name.equals("thermionic_fabricator") && FabricLoader.getInstance().isModLoaded("jei")) {
                    context.runOnClient(F1JeiProbe::transferFabricator);
                    context.waitTicks(10);
                    world.getServer().runOnServer(server -> require(!((forestry.core.content.machines.tiles.TileFabricator) server.overworld().getBlockEntity(pos)).getCraftingInventory().isEmpty(),
                        "Real JEI transfer did not reach the fabricator ghost grid"));
                    context.takeScreenshot("forestry-f3-jei-transfer-fabricator");
                }
                context.setScreen(() -> null);
                if (name.equals("trade_station")) {
                    world.getServer().runOnServer(server -> F2MailTests.openNamedTradeStation(server, pos));
                    context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?> screen
                        && BuiltInRegistries.MENU.getKey(screen.getMenu().getType()).equals(id("trader")));
                    context.waitTicks(8);
                    context.takeScreenshot("forestry-f3-screen-linked-trade-station");
                    context.setScreen(() -> null);
                }
            }
            var handheldMenus = new ArrayList<>(List.of("portable_analyzer", "soldering_iron", "miner_backpack", "apiarists_backpack"));
            if (FabricLoader.getInstance().isModLoaded("forestrymail")) handheldMenus.addAll(List.of("catalog", "letter_empty_fresh"));
            for (String name : handheldMenus) {
                world.getServer().runOnServer(server -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.closeContainer();
                    var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id(name)));
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                    player.inventoryMenu.broadcastChanges();
                });
                context.waitFor(client -> client.player.getMainHandItem().is(BuiltInRegistries.ITEM.getValue(id(name))));
                context.runOnClient(client -> client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.MAIN_HAND));
                context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?>);
                context.waitTicks(8);
                context.takeScreenshot("forestry-f3-screen-" + name);
                if (name.equals("portable_analyzer")) {
                    var kinds = new ArrayList<>(List.of("bee", "tree"));
                    if (FabricLoader.getInstance().isModLoaded("forestrybutterflies")) kinds.add("butterfly");
                    for (String kind : kinds) {
                        for (int page = 0; page < 5; page++) {
                            int selected = page;
                            world.getServer().runOnServer(server -> {
                                var player = server.getPlayerList().getPlayers().getFirst();
                                var inventory = ((forestry.core.content.analyzer.PortableAnalyzerMenu) player.containerMenu).getItemInventory();
                                ItemStack analyzed;
                                if (kind.equals("tree")) {
                                    var individual = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies().createIndividual();
                                    individual.analyze();
                                    analyzed = individual.createStack(forestry.api.arboriculture.genetics.TreeLifeStage.SAPLING);
                                } else if (kind.equals("butterfly")) {
                                    var individual = SpeciesUtil.BUTTERFLY_TYPE.get().getDefaultSpecies().createIndividual();
                                    individual.analyze();
                                    analyzed = individual.createStack(forestry.api.lepidopterology.genetics.ButterflyLifeStage.BUTTERFLY);
                                } else {
                                    var individual = SpeciesUtil.BEE_TYPE.get().getDefaultSpecies().createIndividual();
                                    individual.analyze();
                                    analyzed = individual.createStack(forestry.api.apiculture.genetics.BeeLifeStage.QUEEN);
                                }
                                for (int slot = forestry.core.platform.inventory.PortableAnalyzerInventory.SLOT_ANALYZE_1;
                                        slot <= forestry.core.platform.inventory.PortableAnalyzerInventory.SLOT_ANALYZE_5; slot++) inventory.setItem(slot, ItemStack.EMPTY);
                                inventory.setItem(forestry.core.platform.inventory.PortableAnalyzerInventory.SLOT_ANALYZE_1 + selected, analyzed);
                            });
                            context.waitTicks(8);
                            context.takeScreenshot("forestry-f3-analyzer-" + kind + "-page-" + page);
                        }
                    }
                }
                context.setScreen(() -> null);
            }
            if (FabricLoader.getInstance().isModLoaded("jei")) {
                var categories = context.computeOnClient(client -> F1JeiProbe.forestryCategories());
                require(categories.containsAll(List.of("carpenter", "centrifuge", "fabricator", "fermenter", "moistener", "smelter", "squeezer", "still", "rainmaker", "charcoal.pile")), "Missing Forestry JEI categories: " + categories);
                for (String category : categories) {
                    context.runOnClient(client -> F1JeiProbe.showForestryCategory(category));
                    context.waitTicks(8);
                    context.takeScreenshot("forestry-f3-jei-" + category);
                    context.setScreen(() -> null);
                }
            }
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                player.closeContainer();
                player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(BuiltInRegistries.ITEM.getValue(id("spectacles"))));
                level.setBlockAndUpdate(pos, ArboricultureBlocks.LEAVES.defaultState());
                var leaves = (LeavesBlockEntity) level.getBlockEntity(pos);
                var type = SpeciesUtil.TREE_TYPE.get();
                leaves.setTree(type.getSpecies(ForestryTreeSpecies.CHESTNUT).createIndividual());
                TreeUtil.tryMate(leaves, type.getSpecies(ForestryTreeSpecies.OAK).createIndividual());
                level.setBlockAndUpdate(pos.west(), BuiltInRegistries.BLOCK.getValue(id("lime_default_leaves")).defaultBlockState());
                level.setBlockAndUpdate(pos.east(), BuiltInRegistries.BLOCK.getValue(id("chestnut_default_leaves_fruit")).defaultBlockState());
                var boat = new ForestryBoat(level, -3, 100, 3);
                boat.setWoodType(ForestryWoodType.BALSA);
                level.addFreshEntity(boat);
                for (String item : List.of("bucket_honey", "bucket_biomass", "can", "refractory_candle", "tin_chain")) player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.getValue(id(item))));
            });
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(pos);
            context.waitTicks(25);
            context.takeScreenshot("forestry-f3-leaves-eye-boat");
            context.runOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> client.gui.overlay() == null);
            world.getConnection().waitForChunksRender();
            context.waitTicks(20);
            context.takeScreenshot("forestry-f3-reloaded-leaves-eye-boat");
            context.getInput().lookAt(new BlockPos(-3, 100, 3));
            context.waitTicks(10);
            context.takeScreenshot("forestry-f3-boat");
            world.getServer().runOnServer(server -> {
                var level = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                var can = new ItemStack(BuiltInRegistries.ITEM.getValue(id("can")));
                can.set(forestry.core.features.CoreDataComponents.FLUID_CONTENT.get(),
                    forestry.api.core.fluids.FluidVolume.milliBuckets(forestry.core.platform.fluids.ForestryFluids.HONEY.getFluid(), 1000));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, can);
                level.setBlockAndUpdate(new BlockPos(3, 100, 3), BuiltInRegistries.BLOCK.getValue(id("bottler")).defaultBlockState());
                var storage = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(level, new BlockPos(3, 100, 3), net.minecraft.core.Direction.UP);
                try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    require(storage.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(forestry.core.platform.fluids.ForestryFluids.HONEY.getFluid()),
                        forestry.api.core.fluids.FluidUnits.fromMilliBuckets(5000), transaction) > 0, "Machine visual fixture rejected fluid");
                    transaction.commit();
                }
                level.setBlockAndUpdate(new BlockPos(4, 100, 3), forestry.core.platform.fluids.ForestryFluids.HONEY.getFluid().defaultFluidState().createLegacyBlock());
            });
            context.getInput().lookAt(new BlockPos(3, 100, 3));
            context.waitTicks(45);
            context.takeScreenshot("forestry-f3-machine-fluid-container");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                require(player.openMenu((MenuProvider) server.overworld().getBlockEntity(new BlockPos(3, 100, 3))).isPresent(), "Filled bottler menu failed");
            });
            context.waitFor(client -> client.gui.screen() instanceof forestry.core.content.machines.gui.GuiBottler);
            context.waitTicks(8);
            context.takeScreenshot("forestry-f3-filled-fluid-tank");
            context.setScreen(() -> null);
            for (String name : List.of("balsa_sign", "balsa_hanging_sign")) {
                world.getServer().runOnServer(server -> {
                    server.overworld().setBlockAndUpdate(pos.above(), Blocks.STONE.defaultBlockState());
                    server.overworld().setBlockAndUpdate(pos, BuiltInRegistries.BLOCK.getValue(id(name)).defaultBlockState());
                });
                context.waitFor(client -> client.level.getBlockState(pos).is(BuiltInRegistries.BLOCK.getValue(id(name)))
                    && client.level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity);
                context.setScreen(() -> {
                    var entity = (net.minecraft.world.level.block.entity.SignBlockEntity) net.minecraft.client.Minecraft.getInstance().level.getBlockEntity(pos);
                    return name.endsWith("hanging_sign") ? new net.minecraft.client.gui.screens.inventory.HangingSignEditScreen(entity, true, false)
                        : new net.minecraft.client.gui.screens.inventory.SignEditScreen(entity, true, false);
                });
                context.waitTicks(8);
                context.takeScreenshot("forestry-f3-editor-" + name);
                context.setScreen(() -> null);
            }
        }
        Forestry.LOGGER.info("FORESTRY_F3_CLIENT_UI_JEI_RENDER_PASSED");
    }
}
