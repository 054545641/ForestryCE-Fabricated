package forestry.fabric.release;

import forestry.Forestry;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.content.analyzer.PortableAnalyzerScreen;
import forestry.core.content.machines.gui.GuiCarpenter;
import forestry.core.platform.data.ForestryDataMapRuntime;
import forestry.core.platform.util.SpeciesUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReleaseClientProbe implements ClientModInitializer {
    private final Path directory = Path.of(System.getProperty("forestry.releaseProbe.directory"));
    private final long deadline = System.nanoTime() + 240_000_000_000L;
    private int stage;
    private int ticks;
    private long revision;
    private int reportedStage = -1;

    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) throw new IllegalStateException("Release probe must not run in development mode");
        var origin = FabricLoader.getInstance().getModContainer("forestry").orElseThrow().getOrigin().getPaths();
        if (origin.size() != 1 || !origin.getFirst().toString().endsWith(".jar")) throw new IllegalStateException("Release core did not load from a frozen JAR: " + origin);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        try {
            if (this.stage == 99) return;
            if (this.reportedStage != this.stage) {
                this.reportedStage = this.stage;
                Forestry.LOGGER.info("FORESTRY_F3_RELEASE_CLIENT_STAGE {}", this.stage);
            }
            if (System.nanoTime() > this.deadline) throw new AssertionError("Release client timed out at stage " + this.stage);
            if (this.stage == 7) {
                if (client.level == null && ForestryDataMapRuntime.clientSnapshot().revision() == 0 && SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 0) {
                    Files.writeString(this.directory.resolve("client.passed"), "Native release JAR, dedicated connection, menus, genome sync, reload and disconnect passed\n");
                    Forestry.LOGGER.info("FORESTRY_F3_RELEASE_CLIENT_PASSED");
                    this.stage = 99;
                    client.stop();
                }
                return;
            }
            if (client.level == null || client.player == null || client.getConnection() == null || client.gui.overlay() != null) return;
            if (client.getSingleplayerServer() != null) throw new AssertionError("Expected a separate dedicated server process");
            switch (this.stage) {
                case 0 -> {
                    if (ForestryDataMapRuntime.clientSnapshot().revision() == 0) return;
                    require(SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 69 && SpeciesUtil.TREE_TYPE.get().getSpeciesCount() == 50, "Genetics were not synchronized");
                    if ((Integer.getInteger("forestry.releaseProbe.modules", 0) & 1) != 0)
                        require(SpeciesUtil.BUTTERFLY_TYPE.get().getSpeciesCount() == 35, "Butterfly genetics were not synchronized");
                    this.revision = ForestryDataMapRuntime.clientSnapshot().revision();
                    Files.writeString(this.directory.resolve("client.ready"), "joined\n");
                    this.stage = 1;
                }
                case 1 -> {
                    BlockPos pos = new BlockPos(0, 100, 3);
                    if (client.gui.screen() != null || !Files.exists(this.directory.resolve("begin")) || !client.level.getBlockState(pos).is(BuiltInRegistries.BLOCK.getValue(id("carpenter")))
                            || !(client.level.getBlockEntity(pos) instanceof forestry.core.content.machines.tiles.TileCarpenter)
                            || !client.player.getMainHandItem().is(BuiltInRegistries.ITEM.getValue(id("portable_analyzer")))
                            || client.player.distanceToSqr(Vec3.atCenterOf(pos)) > 32 || ++this.ticks < 20) return;
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
                    this.stage = 2;
                    this.ticks = 0;
                }
                case 2 -> {
                    if (!(client.gui.screen() instanceof GuiCarpenter) || ++this.ticks < 12) return;
                    Screenshot.grab(client, false);
                    client.gui.screen().onClose();
                    this.stage = 3;
                    this.ticks = 0;
                }
                case 3 -> {
                    if (client.gui.screen() != null || !client.player.getMainHandItem().is(BuiltInRegistries.ITEM.getValue(id("portable_analyzer")))) return;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    this.stage = 4;
                }
                case 4 -> {
                    if (!(client.gui.screen() instanceof PortableAnalyzerScreen) || ++this.ticks < 12) return;
                    Screenshot.grab(client, false);
                    client.gui.screen().onClose();
                    client.getConnection().sendCommand("forestry bee give " + forestry.api.apiculture.ForestryBeeSpecies.FOREST + " queen");
                    this.stage = 5;
                }
                case 5 -> {
                    boolean genome = false;
                    for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
                        var individual = IIndividualHandlerItem.getIndividual(client.player.getInventory().getItem(slot));
                        if (individual != null && individual.getSpecies().id().equals(forestry.api.apiculture.ForestryBeeSpecies.FOREST)) genome = true;
                    }
                    if (!genome) return;
                    Files.writeString(this.directory.resolve("client.reload"), "menus and genome passed\n");
                    this.stage = 6;
                }
                case 6 -> {
                    if (ForestryDataMapRuntime.clientSnapshot().revision() <= this.revision) return;
                    require(SpeciesUtil.BEE_TYPE.get().getSpeciesCount() == 69, "Reload lost bee species");
                    client.disconnectWithProgressScreen();
                    this.stage = 7;
                }
                default -> throw new AssertionError("Invalid release probe stage");
            }
        } catch (Throwable error) {
            Forestry.LOGGER.error("FORESTRY_F3_RELEASE_CLIENT_FAILED stage=" + this.stage, error);
            try { Files.writeString(this.directory.resolve("client.failed"), error.toString()); }
            catch (java.io.IOException ignored) {}
            this.stage = 99;
            client.stop();
        }
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("forestry", path); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
