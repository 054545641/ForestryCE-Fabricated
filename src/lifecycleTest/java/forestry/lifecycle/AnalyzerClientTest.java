package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.api.IForestryApi;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.content.analyzer.GuiAnalyzer;
import forestry.core.content.analyzer.TileAnalyzer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.concurrent.CompletableFuture;

import static forestry.lifecycle.N5ClientTest.check;

/** Reproduces new bee -> known bee -> new bee using the client's real quick-move packets. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class AnalyzerClientTest {
	private static int ticks, stage, age;
	private static BlockPos position;
	private static CompletableFuture<Void> pending;

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		if (!Boolean.getBoolean("forestry.lifecycle.analyzerTest") || stage == 10) return;
		var client = Minecraft.getInstance();
		if (++ticks > 2400) throw new AssertionError("Analyzer client test timed out");
		if (client.level == null || client.player == null || client.gui.overlay() != null) return;
		if (pending != null) {
			if (!pending.isDone()) return;
			pending.join();
			pending = null;
		}
		if (++age < 20) return;
		age = 0;
		switch (stage) {
			case 0 -> pending = N5ClientTest.server(client, player -> {
				player.setGameMode(GameType.CREATIVE);
				position = player.blockPosition().above(3);
				player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 2.5);
				var analyzer = (TileAnalyzer) N2WorldTest.place(player.level(), position, "analyzer");
				analyzer.getEnergyManager().setEnergyStored(40000);
				analyzer.getTankManager().getTank(0).setFluid(new FluidStack(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:honey")), 1000));
				for (int slot = 9; slot <= 10; slot++) {
					var drop = IForestryApi.INSTANCE.getHiveManager().getDrops(ForestryBeeSpecies.FOREST).getFirst();
					var bee = drop.createIndividual(player.level(), position);
					bee.setPristine(true);
					player.getInventory().setItem(slot, bee.createStack(BeeLifeStage.PRINCESS));
				}
			});
			case 1 -> pending = N5ClientTest.server(client, player ->
				((TileAnalyzer) player.level().getBlockEntity(position)).openGui(player, InteractionHand.MAIN_HAND, position));
			case 2 -> shiftClick(client, 0, false);
			case 3 -> pending = N5ClientTest.server(client, player -> finishNewBee((TileAnalyzer) player.level().getBlockEntity(position), 1000));
			case 4 -> pending = N5ClientTest.server(client, player -> {
				var analyzer = (TileAnalyzer) player.level().getBlockEntity(position);
				player.getInventory().setItem(9, analyzer.removeItem(8, 1));
				analyzer.getEnergyManager().setEnergyStored(40000);
			});
			case 5 -> shiftClick(client, 0, true);
			case 6 -> pending = N5ClientTest.server(client, player -> {
				var analyzer = (TileAnalyzer) player.level().getBlockEntity(position);
				check(analyzer.getItem(0).isEmpty() && !analyzer.getItem(8).isEmpty(), "Known bee immediately reaches output");
				check(analyzer.getEnergyManager().getEnergyStored() == 40000 && analyzer.getTankManager().getTank(0).getFluidAmount() == 900,
					"Known bee costs no additional power or honey");
			});
			case 7 -> shiftClick(client, 1, false);
			case 8 -> pending = N5ClientTest.server(client, player -> finishNewBee((TileAnalyzer) player.level().getBlockEntity(position), 900));
			case 9 -> {
				var screen = (GuiAnalyzer) client.gui.screen();
				for (int slot = 8; slot <= 9; slot++) check(IIndividualHandlerItem.getIndividual(screen.getMenu().getTile().getItem(slot)).isAnalyzed(), "Both outputs synchronize as analyzed");
				LogUtils.getLogger().info("FORESTRY_ANALYZER_CLIENT_READY new_known_new=true quick_move_packets=true honey_consumed=200");
				client.stop();
			}
		}
		stage++;
	}

	private static void shiftClick(Minecraft client, int slot, boolean analyzed) {
		check(client.gui.screen() instanceof GuiAnalyzer, "Production analyzer menu is open");
		var menu = client.player.containerMenu;
		ItemStack input = menu.getSlot(slot).getItem();
		check(!input.isEmpty() && IIndividualHandlerItem.getIndividual(input).isAnalyzed() == analyzed, "Client sees correct analysis state before Shift-click");
		client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player);
	}

	private static void finishNewBee(TileAnalyzer analyzer, int honeyBefore) {
		check(analyzer.getWorkCounter() > 0 && analyzer.getWorkCounter() < 125 && analyzer.getStepsPerWorkCycle() == 125, "Fresh bee is still undergoing the complete 125-step cycle");
		check(!IIndividualHandlerItem.getIndividual(analyzer.getItem(0)).isAnalyzed(), "Fresh bee remains unknown in the working slot");
		check(analyzer.getTankManager().getTank(0).getFluidAmount() == honeyBefore, "Honey is consumed only at completion");
		for (int step = 0; step < 125 && !analyzer.getItem(0).isEmpty(); step++) check(analyzer.processWorkStep(), "Analysis continues");
		check(analyzer.getItem(0).isEmpty() && analyzer.getTankManager().getTank(0).getFluidAmount() == honeyBefore - 100, "Fresh bee completes with one honey charge");
	}
}
