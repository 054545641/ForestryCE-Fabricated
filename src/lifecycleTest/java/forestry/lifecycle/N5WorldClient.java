package forestry.lifecycle;

import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.entities.ForestryBoat;
import forestry.arboriculture.entities.ForestryChestBoat;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.render.NaturalistEyeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static forestry.lifecycle.N5ClientTest.check;

/** Real world rendering and native sign editor round trip, repeated after resources reload. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class N5WorldClient {
    private static final BlockPos LEAVES = new BlockPos(30, 92, 6);
    private static final BlockPos SIGN = new BlockPos(27, 91, 9);
    private static final List<String> MACHINES = List.of("peat_engine", "biogas_engine", "combustion_engine", "clockwork_engine", "solar_engine", "rainmaker", "apiarists_chest", "arborists_chest", "lepidopterists_chest", "analyzer", "escritoire");
    private static int age;
    private static int outlines;
    private static CompletableFuture<Void> pending;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void extract(ExtractLevelRenderStateEvent event) throws Exception {
        if (!Boolean.getBoolean("forestry.lifecycle.n5Client")) return;
        var field = NaturalistEyeRenderer.class.getDeclaredField("OUTLINES"); field.setAccessible(true);
        var values = event.getRenderState().getRenderData((net.minecraft.util.context.ContextKey<List<?>>) field.get(null));
        outlines = values == null ? 0 : values.size();
    }

    static boolean tick(Minecraft client, int pass) throws Exception {
        if (pending != null) {
            if (!pending.isDone()) return false;
            pending.join(); pending = null;
        }
        age++;
        if (age == 1) {
            client.player.closeContainer();
            pending = N5ClientTest.server(client, N5WorldClient::setup);
        }
        if (age == 45) {
            client.player.setYRot(180); client.player.setXRot(18);

            check(client.level.getBlockEntity(LEAVES) instanceof LeavesBlockEntity leaves && leaves.isPollinated(), "Pollinated leaves synchronize to world renderer: " + client.level.getBlockState(LEAVES));
            check(forestry.core.platform.util.GeneticsUtil.hasNaturalistEye(client.player), "Native head slot spectacles synchronize");
        }
        if (age > 45 && age < 90) particles(client);
        if (age == 50) pending = N5ClientTest.server(client, player -> updateDisplays(player, false));
        if (age == 80) {
            check(outlines > 0, "Equipped spectacles submit actual world highlights");
            for (int i = 0; i < MACHINES.size(); i++) {
                var tile = client.level.getBlockEntity(machinePos(i));
                check(tile != null && client.getBlockEntityRenderDispatcher().getRenderer(tile) != null, "Registered world renderer: " + MACHINES.get(i));
                var state = client.getBlockEntityRenderDispatcher().tryExtractRenderState(tile, .5f, null, false);
                check(state != null, "Native extraction of actual block entity: " + MACHINES.get(i));
                if (state instanceof forestry.core.platform.render.DisplayItemRenderState display) check(!display.item.isEmpty(), "Networked specimen produces visible display layers: " + MACHINES.get(i));
            }
            int entities = 0;
            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof ForestryBoat || entity instanceof forestry.lepidopterology.entities.EntityButterfly) {
                    var state = client.getEntityRenderDispatcher().extractEntity(entity, .5f);
                    check(state != null, "Production entity render state extracted");
                    entities++;
                }
            }
            check(entities >= 3, "Boat, chest boat and butterfly reach the client renderer");
            N5ClientTest.screenshot(client, "n5-" + pass + "-world-models-particles-eye.png");
        }
        if (age == 100) pending = N5ClientTest.server(client, player -> {
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            updateDisplays(player, true);
        });
        if (age == 120) {
            check(outlines == 0, "Removing spectacles clears highlights");
            for (int i : new int[]{9, 10}) {
                var state = (forestry.core.platform.render.DisplayItemRenderState) client.getBlockEntityRenderDispatcher().tryExtractRenderState(client.level.getBlockEntity(machinePos(i)), .5f, null, false);
                check(state.item.isEmpty(), "Clearing a networked specimen removes stale display layers");
            }
            N5ClientTest.screenshot(client, "n5-" + pass + "-world-no-eye.png");
        }
        if (age == 130 || age == 200) {

            boolean hanging = age == 200;
            pending = N5ClientTest.server(client, player -> {
                player.teleportTo(27.5, 90, 11.5);
                var level = player.level();
                level.setBlockAndUpdate(SIGN, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(SIGN.below(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(SIGN.above(), hanging ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                N2WorldTest.place(level, SIGN, hanging ? "balsa_hanging_sign" : "balsa_sign");
            });
        }
        if (age == 145 || age == 215) pending = N5ClientTest.server(client, player -> {
            var sign = (SignBlockEntity) player.level().getBlockEntity(SIGN);
            sign.setAllowedPlayerEditor(player.getUUID());
            player.openTextEdit(sign, true);
        });
        if (age == 160 || age == 230) {
            check(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen, "Real sign editor opened");
            for (int ch : "林业 N5".codePoints().toArray()) client.gui.screen().charTyped(new net.minecraft.client.input.CharacterEvent(ch));
        }
        if (age == 165 || age == 235) N5ClientTest.screenshot(client, "n5-" + pass + (age == 165 ? "-sign.png" : "-hanging-sign.png"));
        if (age == 175 || age == 245) client.gui.screen().onClose();
        if (age == 190 || age == 260) pending = N5ClientTest.server(client, player -> {
            var sign = (SignBlockEntity) player.level().getBlockEntity(SIGN);
            check(sign.getText(true).getMessage(0, false).getString().equals("林业 N5"), "Native sign text packet saves Unicode on server");
        });
        if (age == 275) {
            check(((SignBlockEntity) client.level.getBlockEntity(SIGN)).getText(true).getMessage(0, false).getString().equals("林业 N5"), "Server sign text synchronizes back");
            com.mojang.logging.LogUtils.getLogger().info("FORESTRY_N5_WORLD_READY pass={} machine_renderers=11 signs=2 unicode=true eye_toggle=true particles=true", pass);
            age = 0;
            return true;
        }
        return false;
    }

    private static BlockPos machinePos(int index) { return new BlockPos(24 + (index % 6) * 2, 90, 4 + (index / 6) * 4); }

    private static void setup(ServerPlayer player) {
        var level = player.level();
        level.getServer().clockManager().setTotalTicks(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK).getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD), 6000);
        for (int x = 20; x < 41; x++) for (int z = 0; z < 23; z++) {
            level.setBlockAndUpdate(new BlockPos(x,89,z), Blocks.SMOOTH_STONE.defaultBlockState());
            for (int y = 90; y < 96; y++) level.setBlockAndUpdate(new BlockPos(x,y,z), Blocks.AIR.defaultBlockState());
        }
        for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(20,90,0,41,96,23), e -> !(e instanceof net.minecraft.world.entity.player.Player))) entity.discard();
        player.teleportTo(29.5, 92, 19.5);
        player.getAbilities().flying = true; player.onUpdateAbilities();
        player.setItemSlot(EquipmentSlot.HEAD, forestry.core.features.CoreItems.SPECTACLES.stack());
        for (int i = 0; i < MACHINES.size(); i++) {
            var tile = N2WorldTest.place(level, machinePos(i), MACHINES.get(i));
            if (tile instanceof forestry.core.content.energy.tiles.EngineBlockEntity engine) {
                level.setBlockAndUpdate(machinePos(i), engine.getBlockState().setValue(forestry.core.content.energy.blocks.EngineBlock.VERTICAL_FACING, Direction.values()[i]));
            }
            if (tile instanceof forestry.core.content.analyzer.TileAnalyzer analyzer) analyzer.setItem(0, SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST).createStack(BeeLifeStage.QUEEN));
        }
        var leaves = (LeavesBlockEntity) N2WorldTest.place(level, LEAVES, "leaves");
        // Keep the unsupported display leaves from randomly decaying during the render sweep.
        level.setBlockAndUpdate(LEAVES, leaves.getBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        leaves.setTree(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual());
        leaves.setMate(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.BIRCH).createIndividual());
        var fruit = (LeavesBlockEntity) N2WorldTest.place(level, LEAVES.east(2), "leaves");
        level.setBlockAndUpdate(LEAVES.east(2), fruit.getBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        var cherry = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.SOUR_CHERRY).createIndividual();
        fruit.setTree(cherry); fruit.setFruit(cherry, true); fruit.addRipeness(1);
        var boat = new ForestryBoat(level, 25, 90, 12); boat.setWoodType(ForestryWoodType.BALSA); level.addFreshEntity(boat);
        var chestBoat = new ForestryChestBoat(level, 33, 90, 12); chestBoat.setWoodType(ForestryWoodType.EBONY); level.addFreshEntity(chestBoat);
        var butterfly = forestry.lepidopterology.entities.EntityButterfly.create(forestry.lepidopterology.features.LepidopterologyEntities.BUTTERFLY.entityType(), level,
            SpeciesUtil.BUTTERFLY_TYPE.get().getDefaultSpecies().createIndividual(), LEAVES.south(5));
        butterfly.setNoAi(true); butterfly.setNoGravity(true); butterfly.setPersistenceRequired(); level.addFreshEntity(butterfly);
    }

    private static void particles(Minecraft client) {
        var level = client.level;
        var engine = client.particleEngine;
        engine.createParticle(new forestry.apiculture.particles.BeeParticleData(forestry.apiculture.particles.ApicultureParticles.BEE_EXPLORER_PARTICLE.get(), LEAVES, 0xFFE040), 28, 92, 11, 0, 0, 0);
        engine.createParticle(forestry.apiculture.particles.ApicultureParticles.SNOW_PARTICLE.get(), 31, 93, 10, 0, 0, 0);
        engine.createParticle(forestry.core.platform.particles.CoreParticles.REFRACTORY_WAX.get(), 32, 92, 11, 0, 0, 0);
        if (age % 10 == 0) {
            forestry.core.platform.client.ParticleRender.addEntitySmokeFX(level, 25, 91, 9);
            forestry.core.platform.client.ParticleRender.addEntityIgnitionFX(level, 26, 91, 9);
        }
        forestry.core.platform.client.ParticleRender.addEntityHoneyDustFX(level, 27, 92, 10);
    }

    private static void updateDisplays(ServerPlayer player, boolean empty) {
        for (int i : new int[]{9, 10}) {
            var tile = player.level().getBlockEntity(machinePos(i));
            var specimen = empty ? ItemStack.EMPTY : SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST).createStack(BeeLifeStage.QUEEN);
            forestry.core.platform.util.NetworkUtil.sendToPlayer(new forestry.core.platform.network.packets.PacketItemStackDisplay(tile.getBlockPos(), specimen), player);
        }
    }
}
