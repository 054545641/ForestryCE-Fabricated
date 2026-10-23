package forestry.n6;

import com.mojang.logging.LogUtils;
import forestry.Forestry;
import forestry.api.IForestryApi;
import forestry.core.manual.ManualAccess;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(N6ReleaseTest.ID)
public final class N6ReleaseTest {
    public static final String ID = "forestryn6test";
    private int ticks;
    private boolean reloaded;
    private int connectedTicks;
    private java.util.concurrent.CompletableFuture<Void> reload;

    public N6ReleaseTest(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener(this::started);
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    public static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void audit() {
        int mask = Integer.getInteger("forestry.n6.modules", 7);
        String[] modules = {"forestrybutterflies", "forestryfarms", "forestrymail"};
        for (int i = 0; i < modules.length; i++) check(ModList.get().isLoaded(modules[i]) == ((mask & (1 << i)) != 0), "Installation mask: " + modules[i]);
        var source = Forestry.class.getProtectionDomain().getCodeSource().getLocation().toString();
        check(source.contains(".jar"), "Forestry must load from the built production jar: " + source);
        check(!ModList.get().isLoaded("modkit") && !ModList.get().isLoaded("forestrydatagen")
            && !ModList.get().isLoaded("forestrygametest") && !ModList.get().isLoaded("forestrylifecycletest"), "No development mods on release classpath");
        for (String absent : new String[]{"thedarkcolour.modkit.data.DataHelper", "forestry.lifecycle.LifecycleTests", "forestry.gametest.ManagerLoadedTest"}) {
            try { Class.forName(absent, false, Forestry.class.getClassLoader()); throw new AssertionError("Development class leaked: " + absent); }
            catch (ClassNotFoundException expected) { }
        }
        check(!ManualAccess.get().isAvailable(), "Unavailable Patchouli adapter remains disabled");
        for (String absentMod : new String[]{"patchouli", "kubejs", "rhino"}) {
            check(!ModList.get().isLoaded(absentMod), "G2 acceptance requires the unavailable integration to be absent: " + absentMod);
        }
        for (String absentClass : new String[]{"vazkii.patchouli.api.PatchouliAPI", "dev.latvian.mods.kubejs.KubeJS",
            "dev.latvian.mods.rhino.Context", "forestry.core.platform.compat.patchouli.PatchouliManualAccess",
            "forestry.core.platform.compat.kubejs.KubeForestryPlugin"}) {
            try { Class.forName(absentClass, false, Forestry.class.getClassLoader()); throw new AssertionError("Unavailable integration class on the release classpath: " + absentClass); }
            catch (ClassNotFoundException expected) { }
        }
        LogUtils.getLogger().info("FORESTRY_G2_OPTIONAL_ABSENT patchouli=true kubejs=true rhino=true manual_fallback=true");
        check(IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes().size() == (((mask & 1) == 0) ? 2 : 3), "Only installed species types load");
        LogUtils.getLogger().info("FORESTRY_N6_JAR_READY mask={} source={}", mask, source);
    }

    private void started(ServerStartedEvent event) {
        audit();
        N6ServerChecks.manual(event.getServer().overworld());
        if (Boolean.getBoolean("forestry.n6.network")) LogUtils.getLogger().info("FORESTRY_N6_NETWORK_LISTENING");
    }

    private void tick(ServerTickEvent.Post event) {
        if (!"server".equals(System.getProperty("forestry.n6.side"))) return;
        if (Boolean.getBoolean("forestry.n6.network")) {
            if (++this.ticks > 7200) throw new AssertionError("N6 network server timed out");
            if (Boolean.getBoolean("forestry.n6.networkReload") && event.getServer().getPlayerCount() > 0) {
                if (++this.connectedTicks == 100) this.reload = event.getServer().reloadResources(event.getServer().getPackRepository().getSelectedIds());
                if (this.reload != null && this.reload.isDone() && !this.reloaded) {
                    this.reload.join(); this.reloaded = true;
                    LogUtils.getLogger().info("FORESTRY_N6_NETWORK_RELOADED");
                }
            }
            if (!Files.exists(Path.of("n6.network.stop"))) return;
            event.getServer().saveEverything(false, true, true);
            try { Files.writeString(Path.of("n6.passed"), "production_jars=true network_server=true world_save=true\n"); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            event.getServer().halt(false);
            return;
        }
        if (++this.ticks == 60) this.reload = event.getServer().reloadResources(event.getServer().getPackRepository().getSelectedIds());
        if (this.reload != null && this.reload.isDone() && !this.reloaded) {
            this.reload.join();
            audit();
            this.reloaded = true;
        }
        if (this.ticks > 120 && this.reloaded) {
            event.getServer().saveEverything(false, true, true);
            try { Files.writeString(Path.of("n6.passed"), "production_jars=true data_reload=true world_save=true\n"); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            LogUtils.getLogger().info("FORESTRY_N6_SERVER_READY");
            event.getServer().halt(false);
        }
        if (this.ticks > 1200) throw new AssertionError("Release server reload timed out");
    }
}
