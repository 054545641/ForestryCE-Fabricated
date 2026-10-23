package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.core.content.escritoire.GuiEscritoire;
import forestry.core.content.escritoire.TileEscritoire;
import forestry.core.features.CoreBlocks;
import forestry.core.platform.block.BlockTypeCoreTesr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in visual regression in the disposable lifecycle world, with the actual server menu. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class EscritoireClientTest {
	private static int ticks;
	private static int worldTicks;
	private static BlockPos position;
	private static boolean finished;

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) throws Exception {
		if (!Boolean.getBoolean("forestry.lifecycle.escritoireTest") || finished) return;
		var client = Minecraft.getInstance();
		if (++ticks > 2400) throw new AssertionError("Escritoire screen test timed out");
		if (client.level == null || client.player == null) return;
		worldTicks++;
		var server = client.getSingleplayerServer();
		var playerId = client.player.getUUID();
		if (worldTicks == 40) {
			server.execute(() -> {
				var player = server.getPlayerList().getPlayer(playerId);
				player.setGameMode(GameType.CREATIVE);
				position = player.blockPosition().east();
				player.level().setBlockAndUpdate(position, CoreBlocks.BASE.get(BlockTypeCoreTesr.ESCRITOIRE).block().defaultBlockState());
			});
		} else if (worldTicks == 80) {
			server.execute(() -> {
				var player = server.getPlayerList().getPlayer(playerId);
				((TileEscritoire) player.level().getBlockEntity(position)).openGui(player, InteractionHand.MAIN_HAND, position);
			});
		} else if (worldTicks >= 100) {
			if (!(client.gui.screen() instanceof GuiEscritoire screen)) throw new AssertionError("Escritoire menu did not open: " + client.gui.screen());
			if (worldTicks < 160 || worldTicks >= 200) moveMouse(client, 5, 5);
			else moveMouse(client, screen.getGuiLeft() + 185, screen.getGuiTop() + 45);
			if (worldTicks == 110) screenshot(client, "escritoire-zh-scale2.png");
			if (worldTicks == 120) {
				for (int i = 0; i < 100; i++) screen.mouseScrolled(screen.getGuiLeft() + 185, screen.getGuiTop() + 45, 0, -1);
				var scroll = GuiEscritoire.class.getDeclaredField("noteScroll");
				scroll.setAccessible(true);
				if (scroll.getInt(screen) <= 0) throw new AssertionError("Long note did not scroll");
			}
			if (worldTicks == 140) screenshot(client, "escritoire-zh-scrolled.png");
			if (worldTicks == 180) screenshot(client, "escritoire-zh-tooltip.png");
			if (worldTicks == 200) {
				client.options.guiScale().set(1);
				client.resizeGui();
				for (int i = 0; i < 100; i++) screen.mouseScrolled(screen.getGuiLeft() + 185, screen.getGuiTop() + 45, 0, 1);
			}
			if (worldTicks == 220) {
				finished = true;
				LogUtils.getLogger().info("FORESTRY_ESCRITOIRE_READY native_font=true scroll=true tooltip=true scales=1,2");
				Screenshot.grab(client.gameDirectory, "escritoire-zh-scale1.png", client.gameRenderer.mainRenderTarget(), 1,
					message -> client.execute(client::stop));
			}
		}
	}

	private static void moveMouse(Minecraft client, int x, int y) throws Exception {
		var window = client.getWindow();
		var xField = MouseHandler.class.getDeclaredField("xpos");
		var yField = MouseHandler.class.getDeclaredField("ypos");
		xField.setAccessible(true);
		yField.setAccessible(true);
		xField.setDouble(client.mouseHandler, (double) x * window.getScreenWidth() / window.getGuiScaledWidth());
		yField.setDouble(client.mouseHandler, (double) y * window.getScreenHeight() / window.getGuiScaledHeight());
	}

	private static void screenshot(Minecraft client, String name) {
		Screenshot.grab(client.gameDirectory, name, client.gameRenderer.mainRenderTarget(), 1, message -> {});
	}
}
