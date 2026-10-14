package forestry.mail.letters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.mail.ILetter;
import forestry.api.mail.IMailAddress;
import forestry.core.platform.inventory.InventoryAdapter;
import forestry.core.platform.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import forestry.mail.letters.MailAddress;

public class Letter implements ILetter {
	public static final Codec<Letter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("processed", false).forGetter(Letter::isProcessed),
		MailAddress.CODEC.fieldOf("sender").forGetter(letter -> MailAddress.copyOf(letter.sender)),
		MailAddress.CODEC.optionalFieldOf("recipient").forGetter(letter -> Optional.ofNullable(letter.recipient).map(MailAddress::copyOf)),
		Codec.STRING.optionalFieldOf("text", "").forGetter(Letter::getText),
		Codec.INT.fieldOf("uid").forGetter(letter -> letter.uid),
		ItemContainerContents.CODEC.optionalFieldOf("inventory", ItemContainerContents.EMPTY).forGetter(Letter::getInventoryContents)
	).apply(instance, Letter::new));

	private static final Random rand = new Random();
	public static final short SLOT_ATTACHMENT_1 = 0;
	public static final short SLOT_ATTACHMENT_COUNT = 18;
	public static final short SLOT_POSTAGE_1 = 18;
	public static final short SLOT_POSTAGE_COUNT = 4;

	private boolean isProcessed = false;

	private IMailAddress sender;
	@Nullable
	private IMailAddress recipient;

	private String text = "";
	private final InventoryAdapter inventory = new InventoryAdapter(22, "INV");
	private final int uid;

	public Letter(IMailAddress sender, @Nullable IMailAddress recipient) {
		this.sender = sender;
		this.recipient = recipient;
		this.uid = rand.nextInt();
	}

	private Letter(boolean processed, MailAddress sender, Optional<MailAddress> recipient, String text, int uid, ItemContainerContents contents) {
		this.isProcessed = processed;
		this.sender = sender;
		this.recipient = recipient.orElse(null);
		this.text = text;
		this.uid = uid;
		for (int i = 0; i < Math.min(this.inventory.getContainerSize(), contents.getSlots()); i++) {
			this.inventory.setItem(i, contents.getStackInSlot(i));
		}
	}

	public Letter copy() {
		return new Letter(this.isProcessed, MailAddress.copyOf(this.sender), Optional.ofNullable(this.recipient).map(MailAddress::copyOf), this.text, this.uid, getInventoryContents());
	}

	private ItemContainerContents getInventoryContents() {
		return ItemContainerContents.fromItems(getInventoryStacks());
	}

	@Override
	public NonNullList<ItemStack> getPostage() {
		return InventoryUtil.getStacks(this.inventory, SLOT_POSTAGE_1, SLOT_POSTAGE_COUNT);
	}

	@Override
	public NonNullList<ItemStack> getAttachments() {
		return InventoryUtil.getStacks(this.inventory, SLOT_ATTACHMENT_1, SLOT_ATTACHMENT_COUNT);
	}

	@Override
	public int countAttachments() {
		int count = 0;
		for (ItemStack stack : getAttachments()) {
			if (!stack.isEmpty()) {
				count++;
			}
		}

		return count;
	}

	@Override
	public void addAttachment(ItemStack itemstack) {
		if (!InventoryUtil.tryAddStack(this.inventory, itemstack, SLOT_ATTACHMENT_1, SLOT_ATTACHMENT_COUNT, true)) throw new IllegalArgumentException("Letter attachments exceed capacity");
	}

	@Override
	public void addAttachments(NonNullList<ItemStack> itemstacks) {
		for (ItemStack stack : itemstacks) {
			addAttachment(stack);
		}
	}

	@Override
	public void invalidatePostage() {
		for (int i = SLOT_POSTAGE_1; i < SLOT_POSTAGE_1 + SLOT_POSTAGE_COUNT; i++) {
            this.inventory.setItem(i, ItemStack.EMPTY);
		}
	}

	@Override
	public void setProcessed(boolean flag) {
		this.isProcessed = flag;
	}

	@Override
	public boolean isProcessed() {
		return this.isProcessed;
	}

	@Override
	public boolean isMailable() {
		// Can't resend an already sent letter
		// Requires at least one recipient
		return !this.isProcessed && this.recipient != null;
	}

	@Override
	public boolean isPostPaid() {
		return PostageUtil.sumPostage(getPostage()) >= requiredPostage();
	}

	@Override
	public int requiredPostage() {

		int required = 1;
		for (ItemStack attach : getAttachments()) {
			if (!attach.isEmpty()) {
				required++;
			}
		}

		return required;
	}

	@Override
	public void addStamps(ItemStack stamps) {
		InventoryUtil.tryAddStack(this.inventory, stamps, SLOT_POSTAGE_1, 4, false);
	}

	@Override
	public boolean hasRecipient() {
		return this.recipient != null && !StringUtils.isBlank(this.recipient.getName());
	}

	@Override
	public void setSender(IMailAddress address) {
		this.sender = address;
	}

	@Override
	public IMailAddress getSender() {
		return this.sender;
	}

	@Override
	public void setRecipient(@Nullable IMailAddress address) {
		this.recipient = address;
	}

	@Override
	@Nullable
	public IMailAddress getRecipient() {
		return this.recipient;
	}

	@Override
	public String getRecipientString() {
		if (this.recipient == null) {
			return "";
		}
		return this.recipient.getName();
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public void addTooltip(List<Component> list) {
		if (StringUtils.isNotBlank(this.sender.getName())) {
			list.add(Component.translatable("for.gui.mail.from")
				.append(": " + this.sender.getName())
				.withStyle(ChatFormatting.GRAY));
		}
		if (this.recipient != null) {
			list.add(Component.translatable("for.gui.mail.to")
				.append(": " + this.getRecipientString())
				.withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Letter letter)) {
			return false;
		}
		if (this.isProcessed != letter.isProcessed || this.uid != letter.uid || !this.sender.equals(letter.sender) || !Objects.equals(this.recipient, letter.recipient) || !this.text.equals(letter.text)) {
			return false;
		}
		for (int i = 0; i < this.inventory.getContainerSize(); i++) {
			if (!ItemStack.matches(this.inventory.getItem(i), letter.inventory.getItem(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.isProcessed, this.sender, this.recipient, this.text, this.uid, ItemStack.hashStackList(getInventoryStacks()));
	}

	private List<ItemStack> getInventoryStacks() {
		List<ItemStack> stacks = new ArrayList<>(this.inventory.getContainerSize());
		for (int i = 0; i < this.inventory.getContainerSize(); i++) {
			stacks.add(this.inventory.getItem(i));
		}
		return stacks;
	}

	public void setInventoryChangedListener(Runnable listener) { this.inventory.setInventoryOwner(listener); }

	@Override
	public net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.item.ItemResource> getTransferHandler() { return this.inventory.getTransferHandler(); }

	// / IINVENTORY
	@Override
	public boolean isEmpty() {
		return this.inventory.isEmpty();
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
		return this.inventory.removeItem(var1, var2);
	}

	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return this.inventory.removeItemNoUpdate(index);
	}

	@Override
	public void setItem(int var1, ItemStack var2) {
        this.inventory.setItem(var1, var2);
	}

	@Override
	public int getMaxStackSize() {
		return this.inventory.getMaxStackSize();
	}

	@Override
	public void setChanged() {
        this.inventory.setChanged();
	}

	@Override
	public boolean stillValid(Player var1) {
		return true;
	}

	@Override
	public void startOpen(net.minecraft.world.entity.ContainerUser var1) {
        this.inventory.startOpen(var1);
	}

	@Override
	public void stopOpen(net.minecraft.world.entity.ContainerUser var1) {
        this.inventory.stopOpen(var1);
	}

	@Override
	public boolean canPlaceItem(int i, ItemStack itemstack) {
		return this.inventory.canPlaceItem(i, itemstack);
	}

	@Override
	public void clearContent() {
        this.inventory.clearContent();
	}
}
