package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.core.content.machines.gui.GuiStill;
import forestry.core.platform.gui.GuiForestry;
import forestry.core.platform.gui.ledgers.Ledger;
import forestry.core.platform.gui.ledgers.LedgerManager;
import forestry.core.platform.gui.ledgers.PowerLedger;
import forestry.core.platform.tile.TilePowered;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Opt-in screenshot of the expanded power ledger with actual synchronized machine values. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class PowerLedgerClientTest {
	private static int ticks, stage, age;
	private static BlockPos position;
	private static CompletableFuture<Void> pending;

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) throws Exception {
		if (!Boolean.getBoolean("forestry.lifecycle.powerTest") || stage == 4) return;
		var client = Minecraft.getInstance();
		if (++ticks > 2400) throw new AssertionError("Power ledger client test timed out");
		if (client.level == null || client.player == null || client.gui.overlay() != null) return;
		if (pending != null) {
			if (!pending.isDone()) return;
			pending.join();
			pending = null;
		}
		if (++age < 30) return;
		age = 0;
		if (stage == 0) {
			client.options.guiScale().set(2);
			client.resizeGui();
			pending = N5ClientTest.server(client, player -> {
				player.setGameMode(GameType.CREATIVE);
				position = player.blockPosition().above(3);
				player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 2.5);
				var still = (TilePowered) N2WorldTest.place(player.level(), position, "still");
				still.getEnergyManager().setEnergyStored(12345);
			});
		} else if (stage == 1) {
			pending = N5ClientTest.server(client, player ->
				((TilePowered) player.level().getBlockEntity(position)).openGui(player, InteractionHand.MAIN_HAND, position));
		} else if (stage == 2) {
			if (!(client.gui.screen() instanceof GuiStill screen)) throw new AssertionError("Still GUI did not open");
			if (screen.getMenu().getTile().getEnergyManager().getEnergyStored() != 12345) throw new AssertionError("Stored energy did not synchronize");
			var managerField = GuiForestry.class.getDeclaredField("ledgerManager");
			managerField.setAccessible(true);
			var ledgersField = LedgerManager.class.getDeclaredField("ledgers");
			ledgersField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var ledgers = (List<Ledger>) ledgersField.get(managerField.get(screen));
			ledgers.stream().filter(PowerLedger.class::isInstance).findFirst().orElseThrow().setFullyOpen();
		} else {
			LogUtils.getLogger().info("FORESTRY_POWER_LEDGER_READY stored=12345 expanded=true");
			Screenshot.grab(client.gameDirectory, "power-ledger.png", client.gameRenderer.mainRenderTarget(), 1,
				message -> client.execute(client::stop));
		}
		stage++;
	}
}
