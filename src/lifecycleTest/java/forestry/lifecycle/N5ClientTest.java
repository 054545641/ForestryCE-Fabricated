package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.core.platform.item.WithScreenItem;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Opt-in real client menus/screens and resource reload in an isolated integrated world. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class N5ClientTest {
    private static final boolean MODELS_ONLY = Boolean.getBoolean("forestry.lifecycle.n5ModelsOnly");
    private static final List<String> BLOCKS = List.of("analyzer", "escritoire", "burn_barrel", "worktable", "genetic_filter",
        "apiary", "bee_house", "apiarists_chest", "arborists_chest", "lepidopterists_chest",
        "peat_engine", "biogas_engine", "combustion_engine", "solar_engine",
        "bottler", "carpenter", "centrifuge", "thermionic_fabricator", "fermenter", "moistener", "smelter", "squeezer", "still",
        "farm_crops_managed", "mailbox", "stamp_collector", "trade_station", "trade_station_linked", "alveary_block", "alveary_sieve", "alveary_swarmer", "alveary_hygroregulator", "stone_brick_farm_gearbox");
    private static final List<String> ITEMS = List.of("portable_analyzer", "soldering_iron", "miner_backpack", "woven_miner_backpack",
        "apiarists_backpack", "arborists_backpack", "lepidopterists_backpack", "letter_empty_fresh", "catalog");
    private static BlockPos POS = new BlockPos(8, 90, 8);
    private static int ticks, age, index, pass;
    private static boolean started, finished, worldDone;
    private static volatile String expectedMenu;
    private record TankSnapshot(FluidStack fluid, int capacity) {}
    private static List<TankSnapshot> expectedTanks = List.of();
    private static CompletableFuture<Void> pending;
    private static final java.util.Set<String> visitedMenus = new java.util.HashSet<>();

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("forestry.lifecycle.n5Client") || finished) return;
        var client = Minecraft.getInstance();
        if (started && client.gui.screen() instanceof net.minecraft.client.gui.screens.DisconnectedScreen) {
            throw new AssertionError("N5 disconnected while opening " + name());
        }
        if (++ticks > 15000) throw new AssertionError("N5 client timeout index=" + index + " pass=" + pass + " screen=" + client.gui.screen());
        if (client.level == null || client.player == null || client.gui.overlay() != null) return;
        if (pending != null) {
            if (!pending.isDone()) return;
            pending.join();
            pending = null;
        }
        if (!started) {
            if (++age < 60) return;
            String actualBackend = com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().backendName();
            check(actualBackend.equalsIgnoreCase(System.getProperty("forestry.lifecycle.n5Graphics", "opengl")), "Requested graphics backend must actually be active: " + actualBackend);
            LogUtils.getLogger().info("FORESTRY_N5_BACKEND {}", actualBackend);
            started = true;
            age = 0;
            client.options.guiScale().set(2);
            client.resizeGui();
            N5VisualChecks.items(client);
            pending = server(client, player -> {
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(POS.getX() + 0.5, POS.getY() + 1, POS.getZ() + 2.5);
                for (var type : forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
                    var tracker = type.getBreedingTracker(player.level(), player.getGameProfile());
                    tracker.load(new net.minecraft.nbt.CompoundTag());
                    if (type == forestry.core.platform.util.SpeciesUtil.BEE_TYPE.get()) for (var species : type.getAllSpecies()) tracker.registerSpecies(species);
                    tracker.syncToPlayer(player);
                }
            });
            return;
        }
        if (!MODELS_ONLY && index < BLOCKS.size() + ITEMS.size()) {
            if (++age == 1) {
                pending = server(client, N5ClientTest::prepare);
                return;
            }
            if (age == 20) {
                if (index < BLOCKS.size()) check(client.level.getBlockEntity(POS) != null, "Fixture block synchronized before opening: " + name());
                pending = server(client, N5ClientTest::open);
                return;
            }
            if (age < 45) return;
            check(client.gui.screen() instanceof AbstractContainerScreen<?>, "Production menu opened: " + name());
            var screen = (AbstractContainerScreen<?>) client.gui.screen();
            check(BuiltInRegistries.MENU.getKey(screen.getMenu().getType()).toString().equals(expectedMenu), "Menu packet type matches server: " + name());
            if (age == 45) {
                if (!expectedTanks.isEmpty()) {
                    var tanks = ((ILiquidTankTile) client.level.getBlockEntity(POS)).getTankManager();
                    for (int i = 0; i < expectedTanks.size(); i++) {
                        var expected = expectedTanks.get(i);
                        check(tanks.getTank(i).getCapacity() == expected.capacity() && FluidStack.matches(tanks.getTank(i).getFluid(), expected.fluid()), "Tank contents and capacity synchronize: " + name() + "/" + i);
                    }
                }
                check(screen.getGuiLeft() >= 0 && screen.getGuiTop() >= 0, "Screen fits configured GUI scale: " + name());
                screenshot(client, "n5-" + pass + "-" + name() + ".png");
                LogUtils.getLogger().info("FORESTRY_N5_MENU_READY pass={} fixture={} menu={} screen={} slots={}", pass, name(), expectedMenu, screen.getClass().getSimpleName(), screen.getMenu().slots.size());
                visitedMenus.add(expectedMenu);
            }
            if (age == 45 && screen instanceof forestry.core.content.sorting.gui.GuiGeneticFilter filter) N5VisualChecks.filter(client, filter);
            if (age == 45 && screen instanceof forestry.core.platform.gui.GuiNaturalistInventory<?> naturalist) N5VisualChecks.naturalist(naturalist);
            if (age == 45 && screen instanceof forestry.mail.gui.LetterScreen letter) N5VisualChecks.letter(client, letter);
            if (age == 50 && (screen instanceof forestry.mail.gui.LetterScreen || screen instanceof forestry.core.platform.gui.GuiNaturalistInventory<?>)) screenshot(client, "n5-" + pass + "-" + name() + "-interaction.png");
            if (age == 46 && screen instanceof forestry.core.content.sorting.gui.GuiGeneticFilter filter) {
                // Close selection before exercising inventory slots beneath it.
                filter.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(1, 1, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
            }
            if (age == 46 && !screen.getMenu().slots.isEmpty()) clickDiamonds(screen);
            if (age == 49 && !screen.getMenu().slots.isEmpty()) {
                check(screen.getMenu().getCarried().is(Items.DIAMOND) && screen.getMenu().getCarried().getCount() == 7, "Native slot click picks up seven diamonds: " + name());
                clickPlayerSlot(screen);
            }
            if (age == 51) pending = server(client, player -> {
                check(player.containerMenu.getCarried().isEmpty(), "Server acknowledges returned stack: " + name());
                var stack = player.getInventory().getItem(9);
                check(stack.is(Items.DIAMOND) && stack.getCount() == 7, "Server inventory retains seven diamonds: " + name());
                if (player.containerMenu instanceof forestry.mail.gui.LetterMenu letter) check(letter.getText().equals("林业 UI"), "Letter Unicode reaches server");
                if (player.containerMenu instanceof forestry.core.platform.gui.INaturalistMenu naturalist) check(naturalist.getScrollRow() == 11, "Naturalist scroll reaches server");
            });
            if (age >= 52) {
                if (!screen.getMenu().slots.isEmpty()) check(screen.getMenu().getCarried().isEmpty(), "Native slot click returns carried stack: " + name());
                client.player.closeContainer();
                index++;
                age = 0;
            }
            return;
        }
        if (!MODELS_ONLY && !worldDone) {
            worldDone = N5WorldClient.tick(client, pass);
            return;
        }
        if (pass == 0) {
            pass = 1;
            worldDone = false;
            index = 0;
            age = 0;
            client.options.guiScale().set(1);
            client.resizeGui();
            client.options.languageCode = "en_us";
            client.getLanguageManager().setSelected("en_us");
            pending = client.reloadResourcePacks();
            pending = pending.thenRunAsync(() -> {
                try { N5VisualChecks.items(client); } catch (Exception e) { throw new RuntimeException(e); }
            }, client);
            return;
        }
        finished = true;
        var missing = BuiltInRegistries.MENU.keySet().stream().filter(id -> id.getNamespace().equals("forestry") && !visitedMenus.contains(id.toString())).toList();
        if (!MODELS_ONLY) check(missing.isEmpty(), "All registered production menu types visited: missing=" + missing);
        Files.writeString(Path.of(client.gameDirectory.toString(), "n5-client.passed"), "menus=" + (MODELS_ONLY ? 0 : BLOCKS.size() + ITEMS.size()) + " passes=2 resource_reload=true models_only=" + MODELS_ONLY + "\n");
        if (MODELS_ONLY) LogUtils.getLogger().info("FORESTRY_N5_MODELS_READY resource_reload=true");
        else LogUtils.getLogger().info("FORESTRY_N5_CLIENT_READY menus={} resource_reload=true", BLOCKS.size() + ITEMS.size());
        client.stop();
    }

    private static void clickDiamonds(AbstractContainerScreen<?> screen) {
        clickPlayerSlot(screen);
    }

    private static void clickPlayerSlot(AbstractContainerScreen<?> screen) {
        var client = Minecraft.getInstance();
        var slot = screen.getMenu().slots.stream().filter(s -> s.container == client.player.getInventory() && s.getContainerSlot() == 9).findFirst().orElseThrow();
        var event = new net.minecraft.client.input.MouseButtonEvent(screen.getGuiLeft() + slot.x + 8, screen.getGuiTop() + slot.y + 8, new net.minecraft.client.input.MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static String name() { return index < BLOCKS.size() ? BLOCKS.get(index) : ITEMS.get(index - BLOCKS.size()); }

    private static void prepare(ServerPlayer player) {
        var level = player.level();
        // Each fixture owns a position: immediate visual packets must not target a different
        // previous machine while vanilla's batched block replacement is still in flight.
        POS = new BlockPos(8 + (index % 6) * 6, 90, 32 + (index / 6) * 6);
        for (int x = -3; x < 6; x++) for (int z = -3; z < 6; z++) level.setBlockAndUpdate(POS.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
        player.teleportTo(POS.getX() + .5, POS.getY(), POS.getZ() - 2.5);
        player.closeContainer();
        player.getInventory().clearContent();
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
        if (index < BLOCKS.size()) {
            for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) level.setBlockAndUpdate(POS.offset(x,y,z), Blocks.AIR.defaultBlockState());
            // The standalone hygroregulator menu is valid too. Leave it unassembled so
            // climate control cannot consume water between the exact tank snapshots.
            if (name().startsWith("alveary_") && !name().equals("alveary_hygroregulator")) {
                for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
                    for (int y = 0; y < 3; y++) N2WorldTest.place(level, POS.offset(x,y,z), "alveary_block");
                    level.setBlockAndUpdate(POS.offset(x,3,z), Blocks.OAK_SLAB.defaultBlockState());
                }
            } else if (name().equals("stone_brick_farm_gearbox")) {
                for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) for (int y = 0; y < 4; y++) N2WorldTest.place(level, POS.offset(x,y,z), "stone_brick_farm_block");
            }
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            var tile = N2WorldTest.place(level, POS, name().equals("trade_station_linked") ? "trade_station" : name());
            check(tile instanceof MenuProvider, "Block provides menu: " + name());
            if (name().equals("trade_station_linked")) {
                var trade = (forestry.mail.tradestation.TradeStationBlockEntity) tile;
                trade.getOwnerHandler().setOwner(player.getGameProfile());
                check(trade.setAddress(new forestry.mail.letters.MailAddress("N5Shop" + level.getGameTime())), "Test shop address linked");
            }
            // A still can process its fluid without item inputs; keep it idle for an exact
            // server/client tank snapshot comparison while other machines show stored power.
            if (tile instanceof TilePowered powered) powered.getEnergyManager().setEnergyStored(name().equals("still") ? 0 : 1200);
            if (tile instanceof ILiquidTankTile liquid && liquid.getTankManager().getTank(0) != null) {
                var tank = liquid.getTankManager().getTank(0);
                BuiltInRegistries.FLUID.stream().filter(f -> f != Fluids.EMPTY && f.isSource(f.defaultFluidState()))
                    .map(f -> new FluidStack(f, 1500)).filter(tank::isFluidValid).findFirst().ifPresent(tank::setFluid);
            }
        } else {
            var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("forestry:" + name())));
            check(stack.getItem() instanceof WithScreenItem, "Item provides production screen: " + name());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        }
    }

    private static void open(ServerPlayer player) {
        expectedTanks = List.of();
        if (index < BLOCKS.size()) {
            var tile = player.level().getBlockEntity(POS);
            if (tile instanceof ILiquidTankTile liquid) {
                var snapshots = new java.util.ArrayList<TankSnapshot>();
                for (int i = 0; i < liquid.getTankManager().size(); i++) {
                    var tank = liquid.getTankManager().getTank(i);
                    snapshots.add(new TankSnapshot(tank.getFluid(), tank.getCapacity()));
                }
                expectedTanks = List.copyOf(snapshots);
            }
            if (tile instanceof forestry.core.platform.tile.TileBase base) base.openGui(player, InteractionHand.MAIN_HAND, POS);
            else player.openMenu((MenuProvider) tile, POS);
        }
        else player.getMainHandItem().getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
        expectedMenu = BuiltInRegistries.MENU.getKey(player.containerMenu.getType()).toString();
    }

    static CompletableFuture<Void> server(Minecraft client, java.util.function.Consumer<ServerPlayer> action) {
        var result = new CompletableFuture<Void>();
        var id = client.player.getUUID();
        client.getSingleplayerServer().execute(() -> {
            try { action.accept(client.getSingleplayerServer().getPlayerList().getPlayer(id)); result.complete(null); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result;
    }

    static void screenshot(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1, message -> {});
    }

    static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
