package forestry.core.content.resources;

import forestry.api.IForestryApi;
import forestry.core.platform.network.packets.PacketRefractoryWax;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import forestry.core.platform.item.ItemForestry;

public class ItemRefractoryWax extends ItemForestry {
	public ItemRefractoryWax(Properties properties) {
		super(properties);
	}

	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Block waxed = IForestryApi.INSTANCE.getTreeManager().getRefractoryWaxed(state.getBlock());

		if (waxed != null) {
			BlockState waxedState = waxed.withPropertiesOf(state);
			Player player = context.getPlayer();
			ItemStack stack = context.getItemInHand();

			if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
			}

			stack.shrink(1);
			level.setBlock(pos, waxedState, 11);
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, waxedState));
			if (!level.isClientSide()) {
				NetworkUtil.sendNetworkPacket(new PacketRefractoryWax(pos), pos, level);
			}

			return (level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME);
		}

		return InteractionResult.PASS;
	}
}
