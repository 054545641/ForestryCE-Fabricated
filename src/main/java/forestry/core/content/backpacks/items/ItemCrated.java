package forestry.core.content.backpacks.items;

import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.ItemStackUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public class ItemCrated extends ItemForestry implements ITintedItem {
	private final Supplier<ItemStack> contained;

	public ItemCrated(Supplier<ItemStack> contained, Properties properties) {
		super(properties);
		this.contained = contained;
	}

	public ItemStack getContained() {
		return this.contained.get();
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		ItemStack heldItem = playerIn.getItemInHand(handIn);
		if (!worldIn.isClientSide()) {
			ItemStack contained = this.contained.get();
			if (contained.isEmpty() || heldItem.isEmpty()) {
				return InteractionResult.PASS;
			}

			heldItem.shrink(1);

			ItemStack dropStack = contained.copy();
			dropStack.setCount(9);
			ItemStackUtil.dropItemStackAsEntity(dropStack, worldIn, playerIn.getX() - playerIn.getBbWidth() / 2f, playerIn.getY(), playerIn.getZ() - playerIn.getBbWidth() / 2f, 40);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack itemstack) {
		ItemStack contained = this.contained.get();
		if (contained.isEmpty()) {
			return Component.translatable("item.forestry.crate");
		} else {
			Component containedName = contained.getHoverName();
			return Component.translatable("for.item.crated.grammar", containedName);
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getColorFromItemStack(ItemStack stack, int renderPass) {
		ItemStack contained = this.contained.get();

		if (contained.isEmpty() || renderPass == 0) {
			return -1;
		}

		return contained.getItem() instanceof ITintedItem tinted ? tinted.getColorFromItemStack(contained, renderPass - 1)
			: contained.is(net.minecraft.world.item.Items.GRASS_BLOCK) ? net.minecraft.world.level.GrassColor.getDefaultColor() : -1;
	}
}
