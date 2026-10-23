package forestry.n6;

import com.mojang.logging.LogUtils;
import forestry.core.features.CoreItems;
import forestry.core.platform.tile.TileBase;
import forestry.core.platform.util.GeneticsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

import static forestry.n6.N6ReleaseTest.check;

@EventBusSubscriber(modid = N6ReleaseTest.ID, value = Dist.CLIENT)
public final class N6ClientTest {
    private static final BlockPos POS = new BlockPos(8, 90, 8);
    private static int ticks, age, stage, category, fixture, pass;
    private static boolean started, done;
    private static CompletableFuture<Void> pending;
    private static NonNullList<ItemStack> expected;
    private static String categoryName;
    private static int outlines;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void extract(net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent event) throws Exception {
        var field = forestry.core.platform.render.NaturalistEyeRenderer.class.getDeclaredField("OUTLINES");
        field.setAccessible(true);
        var values = event.getRenderState().getRenderData((net.minecraft.util.context.ContextKey<java.util.List<?>>) field.get(null));
        outlines = values == null ? 0 : values.size();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!"client".equals(System.getProperty("forestry.n6.side")) || done) return;
        if (Boolean.getBoolean("forestry.n6.network")) { N6NetworkClient.tick(); return; }
        var client = Minecraft.getInstance();
        if (++ticks > 9000) throw new AssertionError("N6 client timeout stage=" + stage + " screen=" + client.gui.screen());
        if (client.level == null || client.player == null || client.gui.overlay() != null) return;
        if (pending != null) {
            if (!pending.isDone()) return;
            pending.join(); pending = null;
        }
        boolean jei = ModList.get().isLoaded("jei");
        boolean curios = ModList.get().isLoaded("curios");
        if (jei && !N6JeiChecks.ready()) return;
        if (!started) {
            if (++age < 60) return;
            N6ReleaseTest.audit();
            var backend = com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().backendName();
            check(backend.equalsIgnoreCase(System.getProperty("forestry.n6.graphics", "opengl")), "Requested graphics backend is active: " + backend);
            LogUtils.getLogger().info("FORESTRY_N6_BACKEND {}", backend);
            started = true; age = 0;
            pending = server(player -> {
                player.setGameMode(GameType.CREATIVE);
                player.teleportTo(POS.getX() + .5, POS.getY(), POS.getZ() + 2.5);
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) player.level().setBlockAndUpdate(POS.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                player.getInventory().clearContent();
                player.setItemInHand(InteractionHand.MAIN_HAND, CoreItems.FORESTERS_MANUAL.stack());
                CoreItems.FORESTERS_MANUAL.item().use(player.level(), player, InteractionHand.MAIN_HAND);
                check(player.getMainHandItem().getCount() == 1, "Missing-provider manual is not consumed");
            });
            return;
        }
        age++;
        if (stage == 0) {
            if (age == 1) equipmentAssets(client);
            if (age == 1) pending = server(player -> CoreItems.FORESTERS_MANUAL.item().use(player.level(), player, InteractionHand.MAIN_HAND));
            if (age == 20) screenshot("manual-missing");
            if (age < 30) return;
            if (jei) N6JeiChecks.audit();
            stage = 1; age = 0;
        } else if (stage == 1) {
            if (!jei || category >= N6JeiChecks.categoryCount()) { stage = 2; age = 0; return; }
            if (age == 1) categoryName = N6JeiChecks.show(category);
            if (age == 15) screenshot("jei-" + categoryName);
            if (age >= 20) { client.gui.setScreen(null); category++; age = 0; }
        } else if (stage == 2) {
            if (!jei || fixture >= 4) { stage = 3; age = 0; return; }
            String block = fixture < 2 ? "carpenter" : fixture == 2 ? "thermionic_fabricator" : "worktable";
            if (age == 1) pending = server(player -> {
                player.closeContainer();
                player.level().setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
                player.level().setBlockAndUpdate(POS, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("forestry", block)).defaultBlockState());
                player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 7));
            });
            if (age == 20) pending = server(player -> ((TileBase) player.level().getBlockEntity(POS)).openGui(player, InteractionHand.MAIN_HAND, POS));
            if (age == 40) N6JeiChecks.transfer(fixture, false);
            if (age == 41) pending = server(player -> checkGrid(player, false));
            if (age == 42) N6JeiChecks.showTransfer();
            if (age == 45) expected = N6JeiChecks.clickTransfer(fixture);
            if (age == 60) {
                check(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>, "Successful JEI transfer returns to the original machine screen");
                pending = server(player -> checkGrid(player, true));
            }
            if (age == 65) screenshot("transfer-" + fixture);
            if (age == 66 && fixture < 3) pending = server(player -> {
                var forged = NonNullList.withSize(9, new ItemStack(Items.DIAMOND));
                var remote = new BlockPos(24_000_008, 90, 24_000_008);
                check(!player.level().hasChunkAt(remote), "Remote transfer fixture is unloaded");
                forestry.core.content.machines.network.packets.PacketRecipeTransferRequest.handle(
                    new forestry.core.content.machines.network.packets.PacketRecipeTransferRequest(remote, forged), player);
                check(!player.level().hasChunkAt(remote), "Forged transfer cannot load remote chunks");
                checkGrid(player, true);
                player.closeContainer();
                forestry.core.content.machines.network.packets.PacketRecipeTransferRequest.handle(
                    new forestry.core.content.machines.network.packets.PacketRecipeTransferRequest(POS, forged), player);
                checkGrid(player, true);
            });
            if (age >= 70) { client.player.closeContainer(); fixture++; age = 0; }
        } else if (stage == 3) {
            if (age == 1) {
                client.gui.setScreen(null);
                pending = server(player -> {
                    player.setGameMode(GameType.SURVIVAL);
                    var leafPos = POS.north(2).above();
                    player.level().setBlockAndUpdate(leafPos, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("forestry", "leaves"))
                        .defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
                    var leaves = (forestry.arboriculture.leaves.LeavesBlockEntity) player.level().getBlockEntity(leafPos);
                    leaves.setTree(forestry.core.platform.util.SpeciesUtil.getTreeSpecies(forestry.api.arboriculture.ForestryTreeSpecies.OAK).createIndividual());
                    leaves.setMate(forestry.core.platform.util.SpeciesUtil.getTreeSpecies(forestry.api.arboriculture.ForestryTreeSpecies.BIRCH).createIndividual());
                    if (curios) N6CuriosChecks.equip(player, true, false);
                    else player.setItemSlot(EquipmentSlot.HEAD, CoreItems.SPECTACLES.stack());
                });
            }
            if (age == 30) {
                if (curios) N6CuriosChecks.verifyClient(client.player, true);
                else check(GeneticsUtil.hasNaturalistEye(client.player), "Native spectacles still work without Curios");
                client.player.setYRot(180); client.player.setXRot(0);
                check(outlines > 0, "Equipped spectacles submit actual pollination outlines");
            }
            if (age == 35) {
                screenshot("world-eye");
                client.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
            }
            if (age == 40) screenshot(curios ? "curios-equipped" : "native-equipped");
            if (age == 45) pending = server(player -> {
                if (curios) N6CuriosChecks.equip(player, true, true);
                else player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            });
            if (age == 65) {
                if (curios) N6CuriosChecks.verifyClient(client.player, false);
                else check(!GeneticsUtil.hasNaturalistEye(client.player), "Unequipping clears vision");
                check(outlines == 0, "Cosmetic/removed spectacles do not retain world outlines");
                screenshot(curios ? "curios-cosmetic" : "native-unequipped");
            }
            if (age == 70) pending = server(player -> { if (curios) N6CuriosChecks.equip(player, false, false); });
            if (age == 75) pending = server(player -> {
                player.setItemSlot(EquipmentSlot.HEAD, forestry.apiculture.features.ApicultureItems.APIARIST_HELMET.stack());
                player.setItemSlot(EquipmentSlot.CHEST, forestry.apiculture.features.ApicultureItems.APIARIST_CHEST.stack());
                player.setItemSlot(EquipmentSlot.LEGS, forestry.apiculture.features.ApicultureItems.APIARIST_LEGS.stack());
                player.setItemSlot(EquipmentSlot.FEET, forestry.apiculture.features.ApicultureItems.APIARIST_BOOTS.stack());
            });
            if (age == 95) screenshot("apiarist-armor");
            if (age == 100) pending = server(player -> {
                for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) player.setItemSlot(slot, ItemStack.EMPTY);
            });
            if (age < 110) return;
            client.player.closeContainer();
            if (pass == 0) {
                pass = 1; stage = 0; age = 0; category = 0; fixture = 0;
                client.options.languageCode = "en_us";
                client.getLanguageManager().setSelected("en_us");
                pending = client.reloadResourcePacks();
            } else {
                done = true;
                Files.writeString(client.gameDirectory.toPath().resolve("n6.passed"), "production_jars=true jei=" + jei + " curios=" + curios + " resource_reload=true\n");
                LogUtils.getLogger().info("FORESTRY_N6_CLIENT_READY jei={} curios={}", jei, curios);
                client.stop();
            }
        }
    }

    private static void equipmentAssets(Minecraft client) throws Exception {
        int layers = 0;
        for (var entry : client.getResourceManager().listResources("equipment", id -> id.getNamespace().equals("forestry") && id.getPath().endsWith(".json")).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                var info = net.minecraft.client.resources.model.EquipmentClientInfo.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                    com.google.gson.JsonParser.parseReader(reader)).getOrThrow();
                for (var type : info.layers().entrySet()) for (var layer : type.getValue()) {
                    var texture = layer.getTextureLocation(type.getKey());
                    check(client.getResourceManager().getResource(texture).isPresent(), "Native equipment texture exists: " + texture);
                    layers++;
                }
            }
        }
        check(layers >= 3, "Spectacles and both apiarist armor layers are checked");
        LogUtils.getLogger().info("FORESTRY_N6_EQUIPMENT_READY layers={}", layers);
    }

    private static void checkGrid(ServerPlayer player, boolean transferred) {
        var tile = player.level().getBlockEntity(POS);
        net.minecraft.world.Container grid = tile instanceof forestry.core.content.machines.tiles.TileCarpenter carpenter ? carpenter.getCraftingInventory()
            : tile instanceof forestry.core.content.machines.tiles.TileFabricator fabricator ? fabricator.getCraftingInventory()
            : ((forestry.core.content.worktable.tiles.WorktableTile) tile).getCraftingDisplay();
        for (int i = 0; i < 9; i++) {
            var wanted = transferred && i < expected.size() ? expected.get(i) : ItemStack.EMPTY;
            check(ItemStack.isSameItemSameComponents(grid.getItem(i), wanted), "Server ghost grid matches transfer fixture=" + fixture + " slot=" + i + " actual=" + grid.getItem(i) + " wanted=" + wanted);
        }
        check(player.getInventory().getItem(9).getCount() == 7, "Ghost transfer does not consume inventory");
    }

    static CompletableFuture<Void> server(java.util.function.Consumer<ServerPlayer> action) {
        var client = Minecraft.getInstance();
        var result = new CompletableFuture<Void>();
        var id = client.player.getUUID();
        client.getSingleplayerServer().execute(() -> {
            try { action.accept(client.getSingleplayerServer().getPlayerList().getPlayer(id)); result.complete(null); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result;
    }

    static void screenshot(String name) {
        var client = Minecraft.getInstance();
        Screenshot.grab(client.gameDirectory, "n6-" + pass + "-" + name + ".png", client.gameRenderer.mainRenderTarget(), 1, message -> {});
    }
}

