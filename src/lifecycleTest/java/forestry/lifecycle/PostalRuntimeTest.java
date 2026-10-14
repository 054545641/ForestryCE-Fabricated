package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import forestry.mail.carriers.players.POBoxInfo;
import forestry.mail.carriers.players.POBoxRegistry;
import forestry.mail.carriers.trading.TradeStation;
import forestry.mail.carriers.trading.TradeStationRegistry;
import forestry.mail.letters.MailAddress;
import forestry.mail.postoffice.PostOffice;
import forestry.mail.postoffice.MailboxBlockEntity;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@EventBusSubscriber(modid = LifecycleTests.ID)
public final class PostalRuntimeTest {
	static final BlockPos MAILBOX = new BlockPos(0, 65, 0);
	static final Path EXPECTED = Path.of("postal-expected.nbt");
	static final Path NETWORK_EXPECTED = Path.of("postal-network-expected.nbt");
	static String phase() { return System.getProperty("forestry.lifecycle.postalPhase", ""); }

	@SubscribeEvent
	public static void started(ServerStartedEvent event) throws Exception {
		var server = event.getServer();
		var level = server.overworld();
		if (phase().equals("seed")) {
			// Only reset this harness's three fixture addresses in its dedicated test world.
			var boxes = POBoxRegistry.getOrCreate(level);
			for (String name : java.util.List.of("PersistDTo", "PersistTBuyer", "PersistTOwner")) {
				boxes.cachedPOBoxes.remove(new MailAddress(PostalTests.profile(name)));
			}
			boxes.setDirty();
			PostalTests.delivery(level, MAILBOX, "PersistD");
			PostalTests.trade(level, MAILBOX.offset(2, 0, 0), "PersistT");
			NbtIo.writeCompressed(PostalTests.snapshot(level), EXPECTED);
			server.saveEverything(false, true, true);
			LogUtils.getLogger().info("FORESTRY_POSTAL_SAVE_READY real_delivery=true real_trade=true");
			passed();
			server.halt(false);
		} else if (phase().equals("verify")) {
			PostalTests.check(Files.exists(EXPECTED), "Run postalPhase=seed first");
			CompoundTag expected = NbtIo.readCompressed(EXPECTED, NbtAccounter.unlimitedHeap());
			CompoundTag actual = PostalTests.snapshot(level);
			if (!expected.equals(actual) && Files.exists(NETWORK_EXPECTED)) {
				LogUtils.getLogger().warn("FORESTRY_POSTAL_BASELINE_DIAGNOSTIC network_snapshot_matches={}",
					NbtIo.readCompressed(NETWORK_EXPECTED, NbtAccounter.unlimitedHeap()).equals(actual));
			}
			PostalTests.check(expected.equals(actual), "Postal data differs from seed baseline; run seed then verify without an intervening network scenario");
			checkReloadedWatchers(level);
			PostalTests.check(expected.equals(PostalTests.snapshot(level)), "Watcher checks restore exact saved contents");
			server.saveEverything(false, true, true);
			LogUtils.getLogger().info("FORESTRY_POSTAL_RESTART_READY mailbox=true trade=true postage=true dirty_callbacks=true");
			passed();
			server.halt(false);
		} else if (phase().equals("network")) {
			POBoxRegistry.getOrCreate(level).getOrCreatePOBox(new MailAddress(PostalTests.profile("PostalTest"))).clearContent();
			POBoxRegistry.getOrCreate(level).getOrCreatePOBox(new MailAddress(PostalTests.profile("NetworkOwner"))).clearContent();
			PostalTests.station(level, "Network", PostalTests.profile("NetworkOwner"));
			for (int x = -3; x <= 3; x++) for (int z = -3; z <= 5; z++) level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState());
			PostalTests.mailbox(level, MAILBOX);
			LogUtils.getLogger().info("FORESTRY_POSTAL_NETWORK_LISTENING");
		} else if (phase().equals("reconnect")) {
			PostalTests.check(NbtIo.readCompressed(NETWORK_EXPECTED, NbtAccounter.unlimitedHeap()).equals(PostalTests.snapshot(level)), "Network delivery persists across dedicated-server restart");
			LogUtils.getLogger().info("FORESTRY_POSTAL_NETWORK_RELOADED");
		}
	}

	@SubscribeEvent
	public static void commands(RegisterCommandsEvent event) {
		if (!phase().equals("network") && !phase().equals("reconnect")) return;
		event.getDispatcher().register(Commands.literal("postaltest")
			.requires(source -> source.getEntity() instanceof ServerPlayer player && player.getGameProfile().name().equals("PostalTest"))
			.then(Commands.argument("step", StringArgumentType.word()).executes(context -> {
				try { networkStep(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "step")); }
				catch (Exception e) { throw new AssertionError("Postal network acceptance failed", e); }
				return 1;
			})));
	}

	@SubscribeEvent
	public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
		if ((phase().equals("network") || phase().equals("reconnect")) && event.getEntity() instanceof ServerPlayer player
			&& player.getGameProfile().name().equals("PostalTest")) player.level().getServer().halt(false);
	}

	private static void networkStep(ServerPlayer player, String step) throws Exception {
		var level = (ServerLevel) player.level();
		var address = new MailAddress(player.getGameProfile());
		var box = POBoxRegistry.getOrCreate(level).getOrCreatePOBox(address);
		var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, MAILBOX, null);
		var tile = (MailboxBlockEntity) level.getBlockEntity(MAILBOX);
		switch (step) {
			case "rollback" -> {
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				player.inventoryMenu.broadcastChanges();
				player.setGameMode(GameType.CREATIVE);
				player.teleportTo(level, 0.5, 65, 3.5, Set.of(), 180, 15, true);
				var before = PostalTests.snapshot(level);
				var source = PostalTests.source(PostalTests.letter(new MailAddress(PostalTests.profile("NetworkSender")), address, new ItemStack(Items.DIAMOND, 3)));
				PostalTests.check(PostalTests.send(source, handler, false) == 1, "Online delivery simulation succeeds");
				PostalTests.check(before.equals(PostalTests.snapshot(level)), "Online simulation rolls back");
				player.sendSystemMessage(Component.literal("POSTAL_ROLLBACK_OK"));
			}
			case "commit" -> {
				var outgoing = PostalTests.letter(new MailAddress(PostalTests.profile("NetworkSender")), address, new ItemStack(Items.DIAMOND, 3));
				player.setItemInHand(InteractionHand.MAIN_HAND, outgoing);
				tile.openGui(player, InteractionHand.MAIN_HAND, MAILBOX);
				PostalTests.check(player.getMainHandItem().isEmpty(), "Real player's hand is consumed by mailbox delivery");
				var order = PostalTests.source(PostalTests.letter(address, new MailAddress("NetworkShop"), new ItemStack(Items.EMERALD, 7), new ItemStack(Items.APPLE)));
				PostalTests.check(PostalTests.send(order, handler, true) == 1 && order.isEmpty(), "Online trade delivers a real reply");
				PostalTests.check(box.getPOBoxInfo().equals(new POBoxInfo(1, 1)), "Online recipient has player and trade mail");
				NbtIo.writeCompressed(PostalTests.snapshot(level), NETWORK_EXPECTED);
				level.getServer().saveEverything(false, true, true);
				player.sendSystemMessage(Component.literal("POSTAL_COMMIT_OK"));
			}
			case "mailbox" -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				tile.openGui(player, InteractionHand.MAIN_HAND, MAILBOX);
			}
			case "letter" -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, box.getItem(1).copy());
				player.inventoryMenu.broadcastChanges();
				player.sendSystemMessage(Component.literal("POSTAL_LETTER_EQUIPPED"));
			}
			case "offhand" -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setItemInHand(InteractionHand.OFF_HAND, box.getItem(1).copy());
				player.inventoryMenu.broadcastChanges();
				player.sendSystemMessage(Component.literal("POSTAL_OFFHAND_EQUIPPED"));
			}
			case "done" -> {
				player.closeContainer();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				PostalTests.check(NbtIo.readCompressed(NETWORK_EXPECTED, NbtAccounter.unlimitedHeap()).equals(PostalTests.snapshot(level)), "Opening mail screens does not mutate saved originals");
				level.getServer().saveEverything(false, true, true);
				LogUtils.getLogger().info("FORESTRY_POSTAL_NETWORK_SERVER_READY phase={}", phase());
				passed();
				player.sendSystemMessage(Component.literal("POSTAL_DONE_OK"));
			}
			default -> throw new AssertionError("Unknown postal test step " + step);
		}
	}

	private static void passed() throws java.io.IOException {
		Files.writeString(Path.of("postal-" + phase() + ".passed"), "passed");
	}

	private static void checkReloadedWatchers(ServerLevel level) {
		var boxes = POBoxRegistry.getOrCreate(level);
		var box = boxes.getPOBox(new MailAddress(PostalTests.profile("PersistDTo")));
		PostalTests.check(box != null, "Delivered mailbox restored");
		boxes.setDirty(false);
		var resource = box.getTransferHandler().getResource(0);
		try (var tx = Transaction.openRoot()) { box.getTransferHandler().extract(0, resource, 1, tx); }
		PostalTests.check(!boxes.isDirty(), "Rollback does not dirty reloaded mailbox registry");
		try (var tx = Transaction.openRoot()) { box.getTransferHandler().extract(0, resource, 1, tx); tx.commit(); }
		PostalTests.check(boxes.isDirty(), "Committed extraction dirties reloaded mailbox registry");
		try (var tx = Transaction.openRoot()) { box.getTransferHandler().insert(0, resource, 1, tx); tx.commit(); }
		var trades = TradeStationRegistry.getOrCreate(level);
		var trade = trades.getTradeStation(new MailAddress("PersistTShop"));
		PostalTests.check(trade != null, "Trade station restored");
		trades.setDirty(false);
		var stock = trade.getTransferHandler().getResource(TradeStation.SLOT_SEND_BUFFER);
		try (var tx = Transaction.openRoot()) { trade.getTransferHandler().extract(TradeStation.SLOT_SEND_BUFFER, stock, 1, tx); tx.commit(); }
		PostalTests.check(trades.isDirty(), "Reloaded trade stock updates persistence");
		try (var tx = Transaction.openRoot()) { trade.getTransferHandler().insert(TradeStation.SLOT_SEND_BUFFER, stock, 1, tx); tx.commit(); }
		var office = PostOffice.getOrCreate(level);
		office.setDirty(false);
		ItemStack stamp = office.getAnyStamp(1);
		PostalTests.check(!stamp.isEmpty() && office.isDirty(), "Reloaded postage ledger updates persistence");
		office.collectPostage(NonNullList.of(ItemStack.EMPTY, stamp));
	}
}
