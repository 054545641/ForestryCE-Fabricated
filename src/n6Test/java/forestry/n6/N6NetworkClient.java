package forestry.n6;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen;

import java.nio.file.Files;
import java.util.Map;

import static forestry.n6.N6ReleaseTest.check;

final class N6NetworkClient {
    private static int ticks, age;
    private static boolean done;
    private static int initialGeneration;

    static void tick() throws Exception {
        if (done) return;
        var client = Minecraft.getInstance();
        boolean reject = Boolean.getBoolean("forestry.n6.expectDisconnect");
        if (++ticks > 3600) throw new AssertionError("N6 network client timed out: " + client.gui.screen());
        if (client.gui.screen() instanceof ModMismatchDisconnectedScreen screen) {
            check(reject, "Unexpected mod mismatch");
            var field = ModMismatchDisconnectedScreen.class.getDeclaredField("mismatchedChannelData"); field.setAccessible(true);
            var channels = (Map<Identifier, ?>) field.get(screen);
            check(!channels.isEmpty() && channels.keySet().stream().allMatch(id -> id.getNamespace().equals("curios")), "Only upstream Curios channels prevent asymmetric connection: " + channels);
            if (++age < 20) return;
            N6ClientTest.screenshot("curios-mismatch");
            LogUtils.getLogger().info("FORESTRY_N6_NETWORK_REJECTED {}", channels.keySet());
            finish(client, "curios_asymmetric_rejected=true");
        } else if (client.gui.screen() instanceof DisconnectedScreen screen) {
            throw new AssertionError("Unexpected disconnect: " + screen.getNarrationMessage().getString());
        } else if (client.level != null && client.player != null && client.gui.overlay() == null) {
            check(!reject, "Expected Curios negotiation failure, but joined the server");
            if (ModList.get().isLoaded("jei") && !N6JeiChecks.ready()) return;
            age++;
            if (Boolean.getBoolean("forestry.n6.networkReload")) {
                if (age == 60 && ModList.get().isLoaded("jei")) { N6JeiChecks.audit(); initialGeneration = N6JeiChecks.generation; }
                if (age < 220) return;
                if (ModList.get().isLoaded("jei")) check(N6JeiChecks.generation > initialGeneration, "Real server reload rebuilds JEI runtime");
            } else if (age < 60) return;
            N6ReleaseTest.audit();
            if (ModList.get().isLoaded("jei")) N6JeiChecks.audit();
            N6ClientTest.screenshot("network-joined");
            LogUtils.getLogger().info("FORESTRY_N6_NETWORK_CONNECTED");
            finish(client, "production_jars=true network_connected=true");
        }
    }

    private static void finish(Minecraft client, String result) throws Exception {
        done = true;
        Files.writeString(client.gameDirectory.toPath().resolve("n6.passed"), result + "\n");
        client.stop();
    }
}
