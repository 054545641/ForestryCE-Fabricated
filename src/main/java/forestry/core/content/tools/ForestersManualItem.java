package forestry.core.content.tools;

import forestry.core.manual.ManualAccess;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ForestersManualItem extends forestry.core.platform.item.ItemForestry {
	private static final String MISSING_KEY = "item.forestry.foresters_manual.missing_patchouli";

	public ForestersManualItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player instanceof ServerPlayer serverPlayer) {
			if (ManualAccess.get().isAvailable()) {
				ManualAccess.get().openManual(serverPlayer, this);
				player.playSound(SoundEvents.BOOK_PAGE_TURN, 1F, (float) (0.7 + Math.random() * 0.4));
			} else {
				// no provider: one hint per use, no page sound, the stack is not consumed
				serverPlayer.sendOverlayMessage(Component.translatable(MISSING_KEY));
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		if (!ManualAccess.get().isAvailable()) {
			tooltip.add(Component.translatable(MISSING_KEY + "_hint").withStyle(ChatFormatting.GRAY));
		}
	}
}
