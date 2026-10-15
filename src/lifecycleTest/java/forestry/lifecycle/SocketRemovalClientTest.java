package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.api.IForestryApi;
import forestry.api.core.circuits.ForestryCircuitLayouts;
import forestry.api.core.circuits.ICircuit;
import forestry.core.content.energy.screen.PeatEngineScreen;
import forestry.core.content.energy.tiles.PeatEngineBlockEntity;
import forestry.core.content.resources.EnumElectronTube;
import forestry.core.engine.circuits.EnumCircuitBoardType;
import forestry.core.engine.circuits.ItemCircuitBoard;
import forestry.core.features.CoreItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.concurrent.CompletableFuture;

import static forestry.lifecycle.N5ClientTest.check;

/** Real screen clicks and packets, including the cursor stack and the returned board's client sync. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class SocketRemovalClientTest {
	private static int ticks, age, stage, scenario;
	private static BlockPos position;
	private static ItemStack board;
	private static CompletableFuture<Void> pending;

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		if (!Boolean.getBoolean("forestry.lifecycle.socketTest") || scenario == 4) return;
		var client = Minecraft.getInstance();
		if (++ticks > 2400) throw new AssertionError("Socket removal client test timed out");
		if (client.level == null || client.player == null || client.gui.overlay() != null) return;
		if (pending != null) {
			if (!pending.isDone()) return;
			pending.join();
			pending = null;
		}
		if (++age < 20) return;
		age = 0;
		switch (stage) {
			case 0 -> pending = N5ClientTest.server(client, SocketRemovalClientTest::prepare);
			case 1 -> {
				// Teleporting the fixture can outrun the client's chunk delivery.
				if (!(client.level.getBlockEntity(position) instanceof PeatEngineBlockEntity)) return;
				pending = N5ClientTest.server(client, player ->
					((PeatEngineBlockEntity) player.level().getBlockEntity(position)).openGui(player, InteractionHand.MAIN_HAND, position));
			}
			case 2 -> pickup(client, 0);
			case 3 -> clickSocket(client, 0);
			case 4 -> {
				check(ItemStack.matches(screen(client).getMenu().getTile().getSocket(0), board), "Installed board synchronizes to the screen");
				check(client.player.containerMenu.getCarried().isEmpty(), "Installation consumes the carried board");
				if (scenario < 2) pickup(client, 1);
			}
			case 5 -> clickSocket(client, scenario % 2);
			case 6 -> {
				boolean removed = scenario < 2;
				ItemStack socket = screen(client).getMenu().getTile().getSocket(0);
				check(removed ? socket.isEmpty() : ItemStack.matches(socket, board), "Only a cursor iron removes the client's board");
				check(countBoards(client.player.getInventory()) == (removed ? 1 : 0), "The board returns only after an allowed removal");
				ItemStack iron = switch (scenario) {
					case 0, 1 -> client.player.containerMenu.getCarried();
					case 2 -> client.player.getMainHandItem();
					default -> client.player.getOffhandItem();
				};
				check(iron.is(CoreItems.SOLDERING_IRON.item()) && iron.getDamageValue() == (scenario == 1 ? 1 : 0), "Only allowed survival removal damages the cursor iron");
				pending = N5ClientTest.server(client, player -> {
					ItemStack serverSocket = ((PeatEngineBlockEntity) player.level().getBlockEntity(position)).getSocket(0);
					check(removed ? serverSocket.isEmpty() : ItemStack.matches(serverSocket, board), "Server enforces cursor-only removal");
					check(countBoards(player.getInventory()) == (removed ? 1 : 0), "Server conserves the board and its four circuits");
				});
			}
			case 7 -> {
				LogUtils.getLogger().info("FORESTRY_SOCKET_SCENARIO_PASSED scenario={}", scenario);
				if (++scenario == 4) {
					LogUtils.getLogger().info("FORESTRY_SOCKET_CLIENT_READY cursor_left=true cursor_right=true main_hand_rejected=true off_hand_rejected=true board_sync=true");
					Screenshot.grab(client.gameDirectory, "socket-removal.png", client.gameRenderer.mainRenderTarget(), 1,
						message -> client.execute(client::stop));
				}
			}
		}
		stage = (stage + 1) % 8;
	}

	private static void prepare(ServerPlayer player) {
		player.closeContainer();
		player.setGameMode(scenario == 0 ? GameType.CREATIVE : GameType.SURVIVAL);
		player.getInventory().clearContent();
		player.getInventory().setSelectedSlot(0);
		position = new BlockPos(8 + scenario * 4, 90, 96);
		player.teleportTo(position.getX() + 0.5, position.getY(), position.getZ() + 2.5);
		player.setNoGravity(true);
		N2WorldTest.place(player.level(), position, "peat_engine");
		var circuits = IForestryApi.INSTANCE.getCircuitManager();
		var layout = circuits.getLayout(ForestryCircuitLayouts.ENGINE_UPGRADE);
		ICircuit apatite = circuits.getCircuit(layout, CoreItems.ELECTRON_TUBES.stack(EnumElectronTube.APATITE, 1));
		check(apatite != null, "Apatite engine circuit exists");
		board = ItemCircuitBoard.createCircuitboard(EnumCircuitBoardType.INTRICATE, layout, new ICircuit[]{apatite, apatite, apatite, apatite});
		player.getInventory().setItem(9, board.copy());
		ItemStack iron = CoreItems.SOLDERING_IRON.stack();
		if (scenario < 2) player.getInventory().setItem(10, iron);
		else player.setItemInHand(scenario == 2 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, iron);
	}

	private static PeatEngineScreen screen(Minecraft client) {
		check(client.gui.screen() instanceof PeatEngineScreen, "Production peat engine screen is open");
		return (PeatEngineScreen) client.gui.screen();
	}

	private static void pickup(Minecraft client, int slot) {
		client.gameMode.handleContainerInput(screen(client).getMenu().containerId, slot, 0, ContainerInput.PICKUP, client.player);
	}

	private static void clickSocket(Minecraft client, int button) {
		var screen = screen(client);
		var click = new MouseButtonEvent(screen.getGuiLeft() + 79, screen.getGuiTop() + 52, new MouseButtonInfo(button, 0));
		screen.mouseClicked(click, false);
		screen.mouseReleased(click);
	}

	private static int countBoards(Inventory inventory) {
		int count = 0;
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (ItemStack.isSameItemSameComponents(stack, board)) count += stack.getCount();
		}
		return count;
	}
}
