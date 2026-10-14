package forestry.mail.letters;

import forestry.api.core.IItemSubtype;
import forestry.core.platform.item.WithScreenItem;
import forestry.mail.letters.Letter;
import forestry.mail.letters.LetterUtils;
import forestry.mail.gui.LetterMenu;
import forestry.mail.inventory.LetterInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

public class LetterItem extends WithScreenItem {
	public enum State implements IItemSubtype {
		FRESH, STAMPED, OPENED, EMPTIED;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ENGLISH);
		}
	}

	public enum Size implements IItemSubtype {
		EMPTY, SMALL, BIG;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ENGLISH);
		}
	}

	private final Size size;
	private final State state;

	public LetterItem(Size size, State state, Properties properties) {
		super(properties.stacksTo(64).overrideDescription("item.forestry.letter"));
		this.size = size;
		this.state = state;
	}

	public Size getSize() {
		return this.size;
	}

	public State getState() {
		return this.state;
	}


	@Override
	public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
		ItemStack heldItem = player.getItemInHand(handIn);
		if (heldItem.getCount() == 1) {
			return super.use(worldIn, player, handIn);
		} else {
			player.sendSystemMessage(Component.translatable("for.chat.mail.wrongstacksize"));
			return InteractionResult.FAIL;
		}
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);

		Letter letter = LetterUtils.getLetterData(itemstack);
		if (letter == null) {
			list.add(Component.literal("<")
				.append(Component.translatable("for.gui.blank").append(">"))
				.withStyle(ChatFormatting.GRAY));
			return;
		}

		letter.addTooltip(list);
	}

	@Override
	public AbstractContainerMenu getContainer(int containerId, Player player, ItemStack heldItem) {
		return new LetterMenu(containerId, player, new LetterInventory(player, heldItem));
	}
}
