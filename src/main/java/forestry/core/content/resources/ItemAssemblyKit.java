package forestry.core.content.resources;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;
import forestry.core.platform.item.ItemForestry;

public class ItemAssemblyKit extends ItemForestry {
	private final Supplier<ItemStack> assembled;

	public ItemAssemblyKit(Supplier<ItemStack> assembled, Properties properties) {
		super(properties.stacksTo(24));
		this.assembled = assembled;
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		ItemStack heldItem = playerIn.getItemInHand(handIn);
		if (!worldIn.isClientSide()) {
			heldItem.shrink(1);
			ItemEntity entity = new ItemEntity(worldIn, playerIn.getX(), playerIn.getY(), playerIn.getZ(), this.assembled.get());
			worldIn.addFreshEntity(entity);
		}
		return InteractionResult.SUCCESS;
	}
}
