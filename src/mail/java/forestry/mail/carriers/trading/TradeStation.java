package forestry.mail.carriers.trading;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.*;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.util.InventoryUtil;
import forestry.core.platform.util.ItemStackUtil;
import forestry.core.platform.util.NBTUtilForestry;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterUtils;
import forestry.mail.letters.MailAddress;
import forestry.mail.letters.PostageSelector;
import forestry.mail.letters.PostageUtil;
import forestry.mail.postoffice.PostOffice;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.features.MailItems;
import forestry.mail.inventory.TradeStationInventory;
import forestry.mail.letters.EnumStampDefinition;
import forestry.mail.letters.EnumDeliveryState;
import forestry.mail.letters.ResponseNotMailable;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import forestry.core.platform.inventory.InventoryPlain;
import forestry.core.platform.inventory.wrappers.InventoryMapper;
import forestry.mail.postoffice.PostalTransactions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TradeStation implements ITradeStation {
	public static final short SLOT_TRADEGOOD = 0;
	public static final short SLOT_TRADEGOOD_COUNT = 1;
	public static final short SLOT_EXCHANGE_1 = 1;
	public static final short SLOT_EXCHANGE_COUNT = 4;
	public static final short SLOT_LETTERS_1 = 5;
	public static final short SLOT_LETTERS_COUNT = 6;
	public static final short SLOT_STAMPS_1 = 11;
	public static final short SLOT_STAMPS_COUNT = 4;
	public static final short SLOT_RECEIVE_BUFFER = 15;
	public static final short SLOT_RECEIVE_BUFFER_COUNT = 15;
	public static final short SLOT_SEND_BUFFER = 30;
	public static final short SLOT_SEND_BUFFER_COUNT = 10;
	public static final int SLOT_SIZE = SLOT_TRADEGOOD_COUNT + SLOT_EXCHANGE_COUNT + SLOT_LETTERS_COUNT + SLOT_STAMPS_COUNT + SLOT_RECEIVE_BUFFER_COUNT + SLOT_SEND_BUFFER_COUNT;

	@Nullable
	private GameProfile owner;

	@Nullable
	private IMailAddress address;
	private boolean isVirtual = false;
	private boolean isInvalid = false;
	private final InventoryAdapter inventory = new TradeStationInventory();
	{ this.inventory.setInventoryOwner(this::setDirty); }

	private final Set<Watcher> updateWatchers = new HashSet<>();

	public TradeStation(@Nullable GameProfile owner, IMailAddress address) {
		if (!address.getCarrier().equals(PostalCarriers.TRADER.value())) {
			throw new IllegalArgumentException("TradeStation address must be a trader");
		}

		this.owner = owner;
		this.address = address;
	}

	public TradeStation(CompoundTag tag, HolderLookup.Provider registries) {
		read(tag, registries);
	}

	@Override
	public @Nullable IMailAddress getAddress() {
		return this.address;
	}

	// / SAVING & LOADING
	public CompoundTag save(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		if (this.owner != null) {
			CompoundTag nbt = new CompoundTag();
			NBTUtilForestry.writeGameProfile(nbt, this.owner);
			compoundNBT.put("owner", nbt);
		}

		if (this.address != null) {
			CompoundTag nbt = new CompoundTag();
            this.address.write(nbt, registries);
			compoundNBT.put("address", nbt);
		}

		compoundNBT.putBoolean("VRT", this.isVirtual);
		compoundNBT.putBoolean("IVL", this.isInvalid);
        this.inventory.write(compoundNBT, registries);
		return compoundNBT;
	}

	@Override
	public CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
		return save(nbt, registries);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries) {
		if (nbt.contains("owner")) {
            this.owner = NBTUtilForestry.readGameProfile(nbt.getCompoundOrEmpty("owner"));
		}

		if (nbt.contains("address")) {
            this.address = new MailAddress(nbt.getCompoundOrEmpty("address"));
		}

		this.isVirtual = nbt.getBooleanOr("VRT", false);
		this.isInvalid = nbt.getBooleanOr("IVL", false);
        this.inventory.read(nbt, registries);
	}

	/* INVALIDATING */
	@Override
	public boolean isValid() {
		return !this.isInvalid && this.address != null && this.address.isValid();
	}

	@Override
	public void invalidate() {
		this.isInvalid = true;
	}

	/* INFORMATION */
	@Override
	public void setVirtual(boolean isVirtual) {
		this.isVirtual = isVirtual;
		setDirty();
	}

	@Override
	public boolean isVirtual() {
		return this.isVirtual;
	}

	@Override
	public TradeStationInfo getTradeInfo() {
		List<ItemStack> condensedRequired = ItemStackUtil.condenseStacks(InventoryUtil.getStacks(this.inventory, SLOT_EXCHANGE_1, SLOT_EXCHANGE_COUNT));

		// Set current state
		EnumTradeStationState state = EnumTradeStationState.OK;

		// Virtual trade stations are always ready for service.
		if (!isVirtual()) {
			// Will assume that the owner should get a notice.
			if (!hasPaper(2)) {
				state = EnumTradeStationState.INSUFFICIENT_PAPER;
			}

			if (!canPayPostage(3)) {
				state = EnumTradeStationState.INSUFFICIENT_STAMPS;
			}

			if (countFillableOrders(1, this.inventory.getItem(SLOT_TRADEGOOD)) <= 0) {
				state = EnumTradeStationState.INSUFFICIENT_TRADE_GOOD;
			}
		}

		return new TradeStationInfo(this.address, this.owner, this.inventory.getItem(SLOT_TRADEGOOD), condensedRequired, state);
	}

	/* ILETTERHANDLER */
	@Override
	public IPostalState handleLetter(ServerLevel world, IMailAddress recipient, ItemStack letterstack, TransactionContext transaction) {
		return PostalTransactions.attempt(transaction, true, tx -> fulfill(world, letterstack, tx));
	}

	private IPostalState fulfill(ServerLevel world, ItemStack letterstack, TransactionContext transaction) {
		ILetter incoming = LetterUtils.getLetter(letterstack);
		if (!isValid() || incoming == null || !incoming.getSender().isValid()) return EnumDeliveryState.NOT_MAILABLE;
		boolean notifyOwner = this.owner != null;
		ItemStack goods = this.inventory.getItem(SLOT_TRADEGOOD).copy();
		if (goods.isEmpty()) return EnumTradeStationState.INSUFFICIENT_TRADE_GOOD;
		var requested = InventoryUtil.getStacks(this.inventory, SLOT_EXCHANGE_1, SLOT_EXCHANGE_COUNT);
		int maxOrders = Math.max(1, Letter.SLOT_ATTACHMENT_COUNT * goods.getMaxStackSize() / goods.getCount());
		if (!isVirtual()) maxOrders = countFillableOrders(maxOrders, goods);
		TradePayment payment = TradePayment.plan(incoming.getAttachments(), requested, maxOrders);
		if (payment.orders() == 0) return EnumTradeStationState.INSUFFICIENT_OFFER;

		// Find a batch whose response, including all change, fits in a real letter.
		InventoryPlain payload = null;
		while (payment.orders() > 0) {
			InventoryPlain candidate = new InventoryPlain(Letter.SLOT_ATTACHMENT_COUNT, "Attachments", 64);
			boolean fits = InventoryUtil.tryAddStack(candidate, goods.copyWithCount(Math.multiplyExact(goods.getCount(), payment.orders())), true);
			for (ItemStack change : payment.surplus()) fits &= InventoryUtil.tryAddStack(candidate, change, true);
			if (fits && (isVirtual() || canStorePayment(payment.received()))) { payload = candidate; break; }
			payment = TradePayment.plan(incoming.getAttachments(), requested, payment.orders() - 1);
		}
		if (payload == null) return EnumTradeStationState.INSUFFICIENT_BUFFER;
		ILetter reply = new Letter(this.address, incoming.getSender());
		reply.setText(Component.translatable("for.gui.mail.order.attached").getString());
		reply.addAttachments(InventoryUtil.getStacks(payload));
		List<ItemStack> replyStamps = PostageSelector.select(denominations(), reply.requiredPostage());
		for (ItemStack stamp : replyStamps) reply.addStamps(stamp.copy());
		if (!reply.isPostPaid()) return EnumTradeStationState.INSUFFICIENT_STAMPS;

		if (!isVirtual()) {
			if (!removePaper(notifyOwner ? 2 : 1, transaction)) return EnumTradeStationState.INSUFFICIENT_PAPER;
			if (!removeStamps(reply.getPostage(), transaction)) return EnumTradeStationState.INSUFFICIENT_STAMPS;
			var stock = new InventoryMapper(this.inventory, SLOT_SEND_BUFFER, SLOT_SEND_BUFFER_COUNT, false);
			if (!InventoryUtil.deleteExactSet(stock, NonNullList.of(ItemStack.EMPTY, goods.copyWithCount(Math.multiplyExact(goods.getCount(), payment.orders()))))) return EnumTradeStationState.INSUFFICIENT_TRADE_GOOD;
			for (ItemStack paid : payment.received()) {
				if (!InventoryUtil.tryAddStack(this.inventory, paid, SLOT_RECEIVE_BUFFER, SLOT_RECEIVE_BUFFER_COUNT, true)) return EnumTradeStationState.INSUFFICIENT_BUFFER;
			}
		}
		PostOffice office = PostOffice.getOrCreate(world);
		IPostalState response = office.lodgeLetter(world, LetterUtils.createLetterStack(reply), transaction);
		if (!response.isOk()) return new ResponseNotMailable(response);
		if (notifyOwner) {
			ILetter notice = new Letter(this.address, new MailAddress(this.owner));
			String text = Component.translatable(payment.orders() == 1 ? "for.gui.mail.order.filled.one" : "for.gui.mail.order.filled.multiple").getString();
			notice.setText(text.replace("%COUNT", Integer.toString(payment.orders())).replace("%SENDER", incoming.getSender().getName()));
			List<ItemStack> noticeStamps = PostageSelector.select(denominations(), notice.requiredPostage());
			for (ItemStack stamp : noticeStamps) notice.addStamps(stamp.copy());
			if (!notice.isPostPaid() || (!isVirtual() && !removeStamps(notice.getPostage(), transaction))) return EnumTradeStationState.INSUFFICIENT_STAMPS;
			IPostalState confirmation = office.lodgeLetter(world, LetterUtils.createLetterStack(notice), transaction);
			if (!confirmation.isOk()) return new ResponseNotMailable(confirmation);
		}
		return EnumDeliveryState.OK;
	}

	private boolean canStorePayment(List<ItemStack> payment) {
		try (var tx = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
			for (ItemStack stack : payment) if (!InventoryUtil.tryAddStack(this.inventory, stack, SLOT_RECEIVE_BUFFER, SLOT_RECEIVE_BUFFER_COUNT, true)) return false;
			return true;
		}
	}

	/* TRADE LOGIC */
	private int countFillableOrders(int max, ItemStack tradegood) {

		if (tradegood.isEmpty()) {
			return 0;
		}

		// How many orders are fillable?
		float orderCount = 0;

		for (ItemStack stack : InventoryUtil.getStacks(this.inventory, SLOT_SEND_BUFFER, SLOT_SEND_BUFFER_COUNT)) {
			if (stack != null && ItemStack.isSameItemSameComponents(stack, tradegood)) {
				orderCount += stack.getCount() / (float) tradegood.getCount();
				if (orderCount >= max) {
					return max;
				}
			}
		}

		return (int) Math.floor(orderCount);
	}

	public boolean canReceivePayment() {
		InventoryAdapter test = this.inventory.copy();
		NonNullList<ItemStack> payment = InventoryUtil.getStacks(this.inventory, SLOT_EXCHANGE_1, SLOT_EXCHANGE_COUNT);

		return InventoryUtil.tryAddStacksCopy(test, payment, SLOT_RECEIVE_BUFFER, SLOT_RECEIVE_BUFFER_COUNT, true);
	}


	// Checks if this trade station has enough paper.
	private boolean hasPaper(int amountRequired) {
		int amountFound = 0;

		for (ItemStack stack : InventoryUtil.getStacks(this.inventory, SLOT_LETTERS_1, SLOT_LETTERS_COUNT)) {
			if (stack.is(Items.PAPER)) {
				amountFound += stack.getCount();
			}

			if (amountFound >= amountRequired) {
				return true;
			}
		}

		return false;
	}

	// Removes a single paper from the inventory
	private boolean removePaper(int count, TransactionContext transaction) {
		for (int slot = SLOT_LETTERS_1; slot < SLOT_LETTERS_1 + SLOT_LETTERS_COUNT && count > 0; slot++) {
			var resource = getTransferHandler().getResource(slot);
			if (!resource.isEmpty() && resource.is(Items.PAPER)) count -= getTransferHandler().extract(slot, resource, count, transaction);
		}
		return count == 0;
	}

	private boolean canPayPostage(int postage) {
		return PostageUtil.sumPostage(InventoryUtil.getStacks(this.inventory, SLOT_STAMPS_1, SLOT_STAMPS_COUNT)) >= postage;
	}

	private List<PostageSelector.Denomination> denominations() {
		return isVirtual()
			? PostageSelector.virtualDenominations()
			: PostageSelector.heldDenominations(InventoryUtil.getStacks(this.inventory, SLOT_STAMPS_1, SLOT_STAMPS_COUNT));
	}

	private boolean removeStamps(List<ItemStack> stamps, TransactionContext transaction) {
		for (ItemStack stamp : stamps) {
			int remaining = stamp.getCount();
			for (int slot = SLOT_STAMPS_1; slot < SLOT_STAMPS_1 + SLOT_STAMPS_COUNT && remaining > 0; slot++) {
				var held = getTransferHandler().getResource(slot);
				if (!held.isEmpty() && held.is(stamp.getItem())) remaining -= getTransferHandler().extract(slot, held, remaining, transaction);
			}
			if (remaining != 0) return false;
		}
		return true;
	}

	@Override public ResourceHandler<ItemResource> getTransferHandler() { return this.inventory.getTransferHandler(); }
	@Override public void write(net.minecraft.world.level.storage.ValueOutput output) {
		if (this.owner != null) output.child("owner").store(NBTUtilForestry.writeGameProfile(new CompoundTag(), this.owner));
		if (this.address != null) output.child("address").store(this.address.write(new CompoundTag(), net.minecraft.core.RegistryAccess.EMPTY));
		output.putBoolean("VRT", this.isVirtual);
		output.putBoolean("IVL", this.isInvalid);
		this.inventory.write(output);
	}

	/* IINVENTORY */

	@Override
	public boolean isEmpty() {
		return this.inventory.isEmpty();
	}

	public void setDirty() {
        this.updateWatchers.forEach(Watcher::onWatchableUpdate);
	}

	@Override
	public void setItem(int slot, ItemStack itemStack) {
		this.inventory.setItem(slot, itemStack);
		this.setDirty();
	}

	@Override
	public int getContainerSize() {
		return this.inventory.getContainerSize();
	}

	@Override
	public ItemStack getItem(int var1) {
		return this.inventory.getItem(var1);
	}

	@Override
	public ItemStack removeItem(int var1, int var2) {
		ItemStack removed = this.inventory.removeItem(var1, var2);
		if (!removed.isEmpty()) setDirty();
		return removed;
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		ItemStack removed = this.inventory.removeItemNoUpdate(index);
		if (!removed.isEmpty()) setDirty();
		return removed;
	}

	@Override
	public int getMaxStackSize() {
		return this.inventory.getMaxStackSize();
	}

	@Override
	public void setChanged() { setDirty(); }

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void startOpen(net.minecraft.world.entity.ContainerUser player) {
	}

	@Override
	public void stopOpen(net.minecraft.world.entity.ContainerUser player) {
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemStack) {
		return this.inventory.canPlaceItem(i, itemStack);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return this.inventory.getSlotsForFace(side);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction side) {
		return this.inventory.canPlaceItemThroughFace(slot, itemStack, side);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction side) {
		return this.inventory.canTakeItemThroughFace(slot, itemStack, side);
	}

	@Override
	public boolean canSlotAccept(int slotIndex, ItemStack stack) {
		return this.inventory.canSlotAccept(slotIndex, stack);
	}

	@Override
	public boolean isLocked(int slotIndex) {
		return this.inventory.isLocked(slotIndex);
	}

	@Override
	public void clearContent() {
	}

	public boolean registerUpdateWatcher(Watcher updateWatcher) {
		return this.updateWatchers.add(updateWatcher);
	}

	public boolean unregisterUpdateWatcher(Watcher updateWatcher) {
		return this.updateWatchers.remove(updateWatcher);
	}
}
