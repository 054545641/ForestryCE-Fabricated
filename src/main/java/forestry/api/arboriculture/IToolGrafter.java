package forestry.api.arboriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IToolGrafter {
	/**
	 * Called by leaves to determine the increase in sapling droprate.
	 *
	 * @param stack ItemStack containing the grafter.
	 * @param world Minecraft world the player and the target block inhabit.
	 * @param pos   Coordinate of the broken leaf block.
	 * @return Float representing the factor the usual drop chance is to be multiplied by.
	 */
	float getSaplingModifier(ItemStack stack, Level world, Player player, BlockPos pos);
}
