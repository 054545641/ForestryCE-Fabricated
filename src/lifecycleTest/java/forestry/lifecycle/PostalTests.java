package forestry.lifecycle;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.ITradeStation;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.mail.blocks.MailBlockType;
import forestry.mail.carriers.players.POBox;
import forestry.mail.carriers.players.POBoxRegistry;
import forestry.mail.carriers.trading.TradeStation;
import forestry.mail.carriers.trading.TradeStationRegistry;
import forestry.mail.features.MailBlocks;
import forestry.mail.features.MailItems;
import forestry.mail.letters.EnumDeliveryState;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterUtils;
import forestry.mail.letters.MailAddress;
import forestry.mail.postoffice.PostOffice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Runs the actual carriers, data components, mailbox capability and persistent registries. */
final class PostalTests {
	static GameProfile profile(String name) {
		return new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)), name);
	}

	static void delivery(GameTestHelper helper) {
		delivery(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), "D" + UUID.randomUUID().toString().substring(0, 8));
		helper.succeed();
	}

	static void trade(GameTestHelper helper) {
		trade(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), "T" + UUID.randomUUID().toString().substring(0, 8));
		helper.succeed();
	}

	static ResourceHandler<ItemResource> mailbox(ServerLevel level, BlockPos pos) {
		level.setBlockAndUpdate(pos, MailBlocks.BASE.get(MailBlockType.MAILBOX).block().defaultBlockState());
		var handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
		check(handler != null, "Mailbox exposes the actual item capability");
		for (var side : Direction.values()) check(level.getCapability(Capabilities.Item.BLOCK, pos, side) == handler, "Mailbox has one transactional entry on " + side);
		return handler;
	}

	static ItemStack letter(IMailAddress from, IMailAddress to, ItemStack... attachments) {
		var letter = new Letter(from, to);
		letter.setText("Postal acceptance: 邮件正文与组件");
		for (var attachment : attachments) letter.addAttachment(attachment.copy());
		letter.addStamps(MailItems.STAMPS.stack(EnumStampDefinition.P_1, letter.requiredPostage()));
		check(letter.isPostPaid(), "Real stamp data map is loaded");
		return LetterUtils.createLetterStack(letter);
	}

	static InventoryAdapter source(ItemStack letter) {
		var source = new InventoryAdapter(1, "PostalSource");
		source.setItem(0, letter.copy());
		return source;
	}

	static int send(InventoryAdapter source, ResourceHandler<ItemResource> mailbox, boolean commit) {
		try (var tx = Transaction.openRoot()) {
			int moved = ResourceHandlerUtil.move(source.getTransferHandler(), mailbox, resource -> true, 1, tx);
			if (commit) tx.commit();
			return moved;
		}
	}

	static void delivery(ServerLevel level, BlockPos pos, String prefix) {
		var handler = mailbox(level, pos);
		var sender = new MailAddress(profile(prefix + "From"));
		var recipient = new MailAddress(profile(prefix + "To"));
		var registry = POBoxRegistry.getOrCreate(level);
		var office = PostOffice.getOrCreate(level);
		var attachment = new ItemStack(Items.DIAMOND, 3);
		attachment.set(DataComponents.CUSTOM_NAME, Component.literal("Postal specimen"));
		var outgoing = letter(sender, recipient, attachment);
		var source = source(outgoing);
		var before = snapshot(level);
		check(send(source, handler, false) == 1, "Simulated actual mailbox accepts a letter");
		check(before.equals(snapshot(level)) && ItemStack.matches(source.getItem(0), outgoing), "Simulation rolls back source, new mailbox and postage");
		check(registry.getPOBox(recipient) == null, "Rolled-back mailbox registration is removed");
		try (var tx = Transaction.openRoot()) {
			check(ResourceHandlerUtil.move(source.getTransferHandler(), handler, resource -> true, 1, tx) == 1, "Nested delivery succeeds before abort");
			throw new IllegalStateException("Injected caller failure");
		} catch (IllegalStateException expected) {
			check("Injected caller failure".equals(expected.getMessage()), "Unexpected transaction exception");
		}
		check(before.equals(snapshot(level)) && ItemStack.matches(source.getItem(0), outgoing), "Exception rollback restores the entire delivery");
		check(send(source, handler, true) == 1 && source.isEmpty(), "Committed delivery consumes the source exactly once");
		var box = registry.getPOBox(recipient);
		check(box != null && box.getPOBoxInfo().playerLetters() == 1, "Real player carrier stores one letter");
		var received = LetterUtils.getLetter(box.getItem(0));
		check(received != null && received.isProcessed() && received.getPostage().stream().allMatch(ItemStack::isEmpty), "Delivered mail is processed and postage invalidated");
		check(ItemStack.matches(received.getAttachments().getFirst(), attachment), "Attachment count and components survive delivery");
		check(!LetterUtils.getLetter(outgoing).isProcessed(), "Outgoing component remains immutable");
		check(office.lodgeLetter(level, box.getItem(0), false) == EnumDeliveryState.ALREADY_MAILED, "Delivered mail cannot be sent twice");
		try (var tx = Transaction.openRoot()) {
			check(handler.extract(0, ItemResource.of(box.getItem(0)), 1, tx) == 0, "Automation cannot extract private mail");
		}
		var unpaid = new Letter(sender, recipient);
		unpaid.setText("Unpaid");
		check(office.lodgeLetter(level, LetterUtils.createLetterStack(unpaid), true) == EnumDeliveryState.NOT_POSTPAID, "Unpaid letters rejected");
		check(office.lodgeLetter(level, letter(sender, new MailAddress(prefix + "Missing")), true) == EnumDeliveryState.NO_MAILBOX, "Unknown trader rejected");
		for (int slot = 0; slot < box.getContainerSize(); slot++) box.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		before = snapshot(level);
		source = source(outgoing);
		check(send(source, handler, true) == 0, "Full mailbox rejects automation");
		check(before.equals(snapshot(level)) && ItemStack.matches(source.getItem(0), outgoing), "Full-mailbox failure loses no source or postage");
		box.clearContent();
		check(send(source, handler, true) == 1, "Delivery recovers after mailbox space is freed");
	}

	static TradeStation station(ServerLevel level, String prefix, GameProfile owner) {
		var station = new TradeStation(owner, new MailAddress(prefix + "Shop"));
		TradeStationRegistry.getOrCreate(level).registerTradeStation(station.getAddress(), station);
		station.setItem(TradeStation.SLOT_TRADEGOOD, new ItemStack(Items.DIAMOND, 2));
		station.setItem(TradeStation.SLOT_EXCHANGE_1, new ItemStack(Items.EMERALD, 3));
		station.setItem(TradeStation.SLOT_SEND_BUFFER, new ItemStack(Items.DIAMOND, 10));
		station.setItem(TradeStation.SLOT_LETTERS_1, new ItemStack(Items.PAPER, 8));
		station.setItem(TradeStation.SLOT_STAMPS_1, MailItems.STAMPS.stack(EnumStampDefinition.P_1, 32));
		return station;
	}

	static void trade(ServerLevel level, BlockPos pos, String prefix) {
		var handler = mailbox(level, pos);
		var buyer = new MailAddress(profile(prefix + "Buyer"));
		var owner = profile(prefix + "Owner");
		var station = station(level, prefix, owner);
		var outgoing = letter(buyer, station.getAddress(), new ItemStack(Items.EMERALD, 7), new ItemStack(Items.APPLE));
		var source = source(outgoing);
		var before = snapshot(level);
		check(send(source, handler, false) == 1, "Real trade and both replies succeed in simulation");
		check(before.equals(snapshot(level)) && ItemStack.matches(source.getItem(0), outgoing), "Trade simulation restores stock, payment, paper, stamps and both replies");
		var ownerBox = POBoxRegistry.getOrCreate(level).getOrCreatePOBox(new MailAddress(owner));
		for (int slot = 0; slot < ownerBox.getContainerSize(); slot++) ownerBox.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		before = snapshot(level);
		check(send(source, handler, true) == 0, "Full owner mailbox rejects trade after buyer reply was prepared");
		check(before.equals(snapshot(level)) && ItemStack.matches(source.getItem(0), outgoing), "Late trade failure rolls back buyer reply, payment and all supplies");
		ownerBox.clearContent();
		station.setItem(TradeStation.SLOT_LETTERS_1, ItemStack.EMPTY);
		before = snapshot(level);
		check(send(source, handler, true) == 0 && before.equals(snapshot(level)), "Missing paper is atomic");
		station.setItem(TradeStation.SLOT_LETTERS_1, new ItemStack(Items.PAPER, 8));
		station.setItem(TradeStation.SLOT_STAMPS_1, ItemStack.EMPTY);
		before = snapshot(level);
		check(send(source, handler, true) == 0 && before.equals(snapshot(level)), "Missing reply postage is atomic");
		station.setItem(TradeStation.SLOT_STAMPS_1, MailItems.STAMPS.stack(EnumStampDefinition.P_1, 32));
		check(send(source, handler, true) == 1 && source.isEmpty(), "Trade commits and consumes one incoming letter");
		var buyerBox = POBoxRegistry.getOrCreate(level).getPOBox(buyer);
		check(buyerBox != null && buyerBox.getPOBoxInfo().tradeLetters() == 1, "Buyer receives one real trade reply");
		var reply = LetterUtils.getLetter(buyerBox.getItem(0));
		check(reply != null && reply.isProcessed(), "Trade reply is processed");
		check(reply.getAttachments().stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 4, "Two orders return four diamonds");
		check(reply.getAttachments().stream().filter(stack -> stack.is(Items.EMERALD)).mapToInt(ItemStack::getCount).sum() == 1, "Trade returns overpayment");
		check(reply.getAttachments().stream().filter(stack -> stack.is(Items.APPLE)).mapToInt(ItemStack::getCount).sum() == 1, "Trade returns unrelated attachment");
		check(count(station, TradeStation.SLOT_RECEIVE_BUFFER, TradeStation.SLOT_RECEIVE_BUFFER_COUNT, Items.EMERALD) == 6, "Only actual payment stored");
		check(count(station, TradeStation.SLOT_SEND_BUFFER, TradeStation.SLOT_SEND_BUFFER_COUNT, Items.DIAMOND) == 6, "Exactly four diamonds consumed");
		check(station.getItem(TradeStation.SLOT_LETTERS_1).getCount() == 6, "Buyer reply and owner notice consume two paper");
		check(ownerBox.getPOBoxInfo().tradeLetters() == 1, "Owner receives one confirmation");
	}

	static int count(Container inventory, int start, int size, Item item) {
		int count = 0;
		for (int slot = start; slot < start + size; slot++) if (inventory.getItem(slot).is(item)) count += inventory.getItem(slot).getCount();
		return count;
	}

	static CompoundTag snapshot(ServerLevel level) {
		var result = new CompoundTag();
		result.put("postage", PostOffice.getOrCreate(level).save(new CompoundTag(), level.registryAccess()));
		var boxes = new CompoundTag();
		POBoxRegistry.getOrCreate(level).cachedPOBoxes.forEach((address, box) -> boxes.put(address.getName(), box.write(new CompoundTag(), level.registryAccess())));
		result.put("boxes", boxes);
		var trades = new CompoundTag();
		TradeStationRegistry.getOrCreate(level).getActiveTradeStations().forEach((address, trade) -> trades.put(address.getName(), trade.write(new CompoundTag(), level.registryAccess())));
		result.put("trades", trades);
		return result;
	}

	static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}
}
