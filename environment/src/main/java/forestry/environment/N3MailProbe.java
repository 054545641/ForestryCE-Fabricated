package forestry.environment;

import forestry.api.mail.IPostOffice;
import forestry.api.mail.IPostalState;
import forestry.mail.letters.EnumDeliveryState;
import forestry.mail.postoffice.*;
import forestry.mail.carriers.trading.TradePayment;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.util.InventoryData;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.RootCommitJournal;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exercises production postal boundaries with probe envelopes, without bootstrapping Forestry registries. */
public final class N3MailProbe {
	private static ItemStack letter() {
		ItemStack letter = new ItemStack(Items.PAPER, 2);
		CompoundTag data = new CompoundTag();
		data.putBoolean("processed", false);
		data.putInt("postage", 2);
		letter.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		ItemStack attachment = new ItemStack(Items.DIAMOND, 7);
		attachment.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved attachment"));
		letter.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(attachment)));
		return letter;
	}

	private static final class DeliveryData extends SavedData implements IPostOffice {
		final Map<String, InventoryAdapter> boxes = new HashMap<>();
		final Object2IntOpenHashMap<Item> stamps = new Object2IntOpenHashMap<>();
		int notifications;
		int dirtyCalls;
		final PostageLedger ledger = new PostageLedger(this.stamps, this::changed);
		final PostalRegistryJournal<String, InventoryAdapter> registry = new PostalRegistryJournal<>(this.boxes,
			box -> box.setInventoryOwner(this::changed), box -> box.setInventoryOwner(() -> {}), this::changed);
		final MailDeliveryHandler handler = new MailDeliveryHandler(DeliveryData::valid, (stack, tx) -> lodgeLetter(null, stack, tx));
		boolean failAfterStore;
		boolean throwAfterStore;
		void changed() { this.dirtyCalls++; setDirty(); }
		static boolean valid(ItemStack stack) {
			var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
			return stack.is(Items.PAPER) && !data.getBooleanOr("processed", false) && data.getIntOr("postage", 0) >= 2;
		}
		@Override public IPostalState lodgeLetter(ServerLevel level, ItemStack stack, TransactionContext transaction) {
			return PostalTransactions.attempt(transaction, true, tx -> {
				if (!valid(stack)) return EnumDeliveryState.NOT_MAILABLE;
				var box = this.boxes.get("recipient");
				if (box == null) {
					this.registry.updateSnapshots(tx);
					box = new InventoryAdapter(1, "Letters", 1);
					box.setInventoryOwner(this::changed);
					this.boxes.put("recipient", box);
				}
				this.ledger.updateSnapshots(tx);
				this.stamps.addTo(Items.GOLD_NUGGET, 2);
				ItemStack processed = stack.copyWithCount(1);
				CompoundTag data = processed.get(DataComponents.CUSTOM_DATA).copyTag();
				data.putBoolean("processed", true);
				data.remove("postage");
				processed.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
				if (box.getTransferHandler().insert(ItemResource.of(processed), 1, tx) != 1) return EnumDeliveryState.MAILBOX_FULL;
				new RootCommitJournal(() -> this.notifications++).updateSnapshots(tx);
				if (this.throwAfterStore) throw new IllegalStateException("Injected carrier failure");
				return this.failAfterStore ? EnumDeliveryState.NO_MAILBOX : EnumDeliveryState.OK;
			});
		}
		@Override public void collectPostage(NonNullList<ItemStack> stamps) {
			try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				this.ledger.updateSnapshots(tx);
				for (ItemStack stamp : stamps) this.stamps.addTo(stamp.getItem(), stamp.getCount());
				tx.commit();
			}
		}
		@Override public ItemStack getAnyStamp(Item item, int max) {
			try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
				int amount = Math.min(Math.max(0, max), this.stamps.getInt(item));
				if (amount == 0) return ItemStack.EMPTY;
				this.ledger.updateSnapshots(tx);
				this.stamps.put(item, this.stamps.getInt(item) - amount);
				tx.commit();
				return new ItemStack(item, amount);
			}
		}
		@Override public ItemStack getAnyStamp(int max) { return getAnyStamp(Items.GOLD_NUGGET, max); }
		CompoundTag save(HolderLookup.Provider registries) {
			CompoundTag tag = new CompoundTag();
			tag.put("collected", this.ledger.writeCollected());
			CompoundTag boxes = new CompoundTag();
			this.boxes.forEach((address, box) -> boxes.put(address, box.write(new CompoundTag(), registries)));
			tag.put("boxes", boxes);
			return tag;
		}
		static DeliveryData load(CompoundTag tag, HolderLookup.Provider registries) {
			DeliveryData data = new DeliveryData();
			data.ledger.readCollected(tag.getCompoundOrEmpty("collected"), (id, count) -> { throw new IllegalArgumentException(id); });
			var boxes = tag.getCompoundOrEmpty("boxes");
			for (String address : boxes.keySet()) {
				var box = new InventoryAdapter(1, "Letters", 1);
				box.read(boxes.getCompoundOrEmpty(address), registries);
				box.setInventoryOwner(data::changed);
				data.boxes.put(address, box);
			}
			return data;
		}
	}

	public static void delivery(GameTestHelper helper) {
		DeliveryData data = new DeliveryData();
		ItemStack original = letter();
		var source = new InventoryAdapter(1, "Source", 64);
		source.setItem(0, original.copy());
		helper.assertTrue(data.lodgeLetter(helper.getLevel(), original, false).isOk(), "Legacy simulate overload executes a real rollback");
		helper.assertTrue(data.boxes.isEmpty() && data.stamps.isEmpty() && data.dirtyCalls == 0 && data.notifications == 0, "Simulation allocates no permanent inbox and collects no postage");
		ItemStack underpaid = original.copy();
		var invalidData = underpaid.get(DataComponents.CUSTOM_DATA).copyTag();
		invalidData.putInt("postage", 0);
		underpaid.set(DataComponents.CUSTOM_DATA, CustomData.of(invalidData));
		ItemStack alreadySent = original.copy();
		invalidData = alreadySent.get(DataComponents.CUSTOM_DATA).copyTag();
		invalidData.putBoolean("processed", true);
		alreadySent.set(DataComponents.CUSTOM_DATA, CustomData.of(invalidData));
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(data.handler.insert(ItemResource.of(underpaid), 1, tx) == 0 && data.handler.insert(ItemResource.of(alreadySent), 1, tx) == 0, "Underpaid and processed probe envelopes are rejected without effects");
			helper.assertTrue(data.handler.insert(ItemResource.of(original), 0, tx) == 0, "Zero insertion has no effects");
			helper.assertTrue(data.handler.insert(ItemResource.of(Items.STONE), 1, tx) == 0, "Non-letter input is rejected");
		}
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(ResourceHandlerUtil.move(source.getTransferHandler(), data.handler, r -> true, 2, tx) == 1, "Virtual slot sends only one letter per insertion");
			helper.assertTrue(data.notifications == 0 && data.dirtyCalls == 0, "No notification or SavedData dirty mark escapes an open transaction");
		}
		helper.assertTrue(source.getItem(0).getCount() == 2 && data.boxes.isEmpty() && data.stamps.isEmpty(), "Outer abort restores sender, receiver registry and postage");
		data.failAfterStore = true;
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(ResourceHandlerUtil.move(source.getTransferHandler(), data.handler, r -> true, 1, tx) == 0, "Late delivery failure rejects insertion");
			tx.commit();
		}
		helper.assertTrue(data.boxes.isEmpty() && data.stamps.isEmpty() && data.notifications == 0 && source.getItem(0).getCount() == 2, "Failed child restores all participants even if parent commits");
		data.failAfterStore = false;
		data.throwAfterStore = true;
		boolean threw = false;
		try (var tx = Transaction.openRoot()) { data.handler.insert(ItemResource.of(original), 1, tx); }
		catch (IllegalStateException expected) { threw = true; }
		helper.assertTrue(threw && data.boxes.isEmpty() && data.stamps.isEmpty() && data.dirtyCalls == 0, "Carrier exception cannot leave a partial delivery");
		data.throwAfterStore = false;
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(ResourceHandlerUtil.move(source.getTransferHandler(), data.handler, r -> true, 1, tx) == 1, "Valid delivery moves one source item");
			tx.commit();
		}
		helper.assertTrue(data.stamps.getInt(Items.GOLD_NUGGET) == 2 && data.notifications == 1 && data.isDirty(), "Successful delivery collects once and notifies only on commit");
		var delivered = data.boxes.get("recipient").getItem(0);
		helper.assertTrue(delivered.get(DataComponents.CUSTOM_DATA).copyTag().getBooleanOr("processed", false), "Recipient receives the processed copy");
		helper.assertTrue(delivered.get(DataComponents.CONTAINER).equals(original.get(DataComponents.CONTAINER)) && ItemStack.matches(original, source.getItem(0).copyWithCount(2)), "Attachments and original envelope components remain intact");
		int dirtyBefore = data.dirtyCalls;
		try (var tx = Transaction.openRoot()) {
			helper.assertTrue(ResourceHandlerUtil.move(source.getTransferHandler(), data.handler, r -> true, 1, tx) == 0, "Full mailbox rejects transfer");
			tx.commit();
		}
		helper.assertTrue(source.getItem(0).getCount() == 1 && data.stamps.getInt(Items.GOLD_NUGGET) == 2 && data.notifications == 1 && data.dirtyCalls == dirtyBefore, "Full receiver preserves sender and committed postage");
		try (var tx = Transaction.openRoot()) { data.getAnyStamp(2); }
		helper.assertTrue(data.stamps.getInt(Items.GOLD_NUGGET) == 2, "Postage withdrawal joins the shared ledger journal");
		helper.succeed();
	}

	public static void trade(GameTestHelper helper) {
		ItemStack named = new ItemStack(Items.DIAMOND, 4);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Payment component"));
		var offered = List.of(named, new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.EMERALD, 2));
		var required = List.of(named.copyWithCount(1), named.copyWithCount(1));
		var payment = TradePayment.plan(offered, required, 99);
		helper.assertTrue(payment.orders() == 2 && payment.received().getFirst().getCount() == 4, "Duplicate requirements are aggregated and limited by real attachments");
		helper.assertTrue(payment.received().getFirst().get(DataComponents.CUSTOM_NAME).equals(named.get(DataComponents.CUSTOM_NAME)), "Payments retain actual components");
		helper.assertTrue(payment.surplus().stream().mapToInt(ItemStack::getCount).sum() == 5 && named.getCount() == 4, "Change and original attachment counts are conserved");
		helper.assertTrue(TradePayment.plan(List.of(new ItemStack(Items.DIAMOND, 64)), required, 99).orders() == 0, "Plain item cannot pay a component-sensitive offer");
		helper.assertTrue(TradePayment.plan(offered, List.of(), 99).orders() == 0, "Empty price cannot mint free orders");
		var stock = new InventoryAdapter(2, "Trade", 64);
		stock.setItem(0, new ItemStack(Items.EMERALD, 10));
		DeliveryData recipient = new DeliveryData();
		IPostalState result;
		try (var tx = Transaction.openRoot()) {
			result = PostalTransactions.attempt(tx, true, batch -> {
				stock.getTransferHandler().extract(0, ItemResource.of(Items.EMERALD), 3, batch);
				stock.getTransferHandler().insert(1, ItemResource.of(named), 4, batch);
				if (!recipient.lodgeLetter(helper.getLevel(), letter(), batch).isOk()) return EnumDeliveryState.MAILBOX_FULL;
				// A later confirmation cannot fit: reply, payment and goods must all roll back.
				return recipient.lodgeLetter(helper.getLevel(), letter(), batch);
			});
			tx.commit();
		}
		helper.assertTrue(!result.isOk() && stock.getItem(0).getCount() == 10 && stock.getItem(1).isEmpty() && recipient.boxes.isEmpty() && recipient.stamps.isEmpty() && recipient.notifications == 0, "Failed confirmation rolls back trade inventory, prior reply and postage");
		helper.succeed();
	}

	public static void persistence(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		var type = new SavedDataType<DeliveryData>(Identifier.parse("forestry_environment:postal_transactions"), DeliveryData::new,
			CompoundTag.CODEC.xmap(tag -> DeliveryData.load(tag, registries), value -> value.save(registries)));
		try {
			var root = Files.createTempDirectory("forestry-n3-mail-");
			try (var storage = new SavedDataStorage(helper.getLevel(), root, DataFixers.getDataFixer(), registries)) {
				DeliveryData data = storage.computeIfAbsent(type);
				helper.assertTrue(data.lodgeLetter(helper.getLevel(), letter(), true).isOk(), "Committed mail is ready for SavedData persistence");
			}
			byte[] committedFile = Files.readAllBytes(type.id().withSuffix(".dat").resolveAgainst(root));
			try (var storage = new SavedDataStorage(helper.getLevel(), root, DataFixers.getDataFixer(), registries)) {
				DeliveryData data = storage.computeIfAbsent(type);
				var stored = data.boxes.get("recipient").getItem(0);
				helper.assertTrue(data.stamps.getInt(Items.GOLD_NUGGET) == 2 && stored.get(DataComponents.CONTAINER).equals(letter().get(DataComponents.CONTAINER)), "SavedData close/reopen restores collected postage and letter attachments");
				helper.assertTrue(!data.isDirty(), "Loading does not manufacture a delivery");
				try (var tx = Transaction.openRoot()) {
					data.boxes.get("recipient").getTransferHandler().extract(0, ItemResource.of(stored), 1, tx);
					data.lodgeLetter(helper.getLevel(), letter(), tx);
				}
				helper.assertTrue(!data.isDirty() && data.notifications == 0, "Aborted replacement stays clean after load");
				CompoundTag malformed = data.ledger.writeCollected();
				malformed.putString("minecraft:gold_nugget", "bad-count");
				boolean rejected = false;
				try { data.ledger.readCollected(malformed, (id, count) -> {}); } catch (IllegalArgumentException expected) { rejected = true; }
				helper.assertTrue(rejected && data.stamps.getInt(Items.GOLD_NUGGET) == 2, "Corrupt postage cannot partially replace the ledger");
			}
			helper.assertTrue(java.util.Arrays.equals(committedFile, Files.readAllBytes(type.id().withSuffix(".dat").resolveAgainst(root))), "Closing after rollback leaves the committed file unchanged");
		} catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
		helper.succeed();
	}
}
