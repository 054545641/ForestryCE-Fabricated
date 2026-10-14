package forestry.arboriculture.charcoal;

import com.mojang.serialization.MapCodec;
import forestry.api.ForestryConstants;
import forestry.core.platform.advancements.AdvancementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import javax.annotation.Nullable;

public class AshBlock extends FallingBlock {
	public static final MapCodec<AshBlock> CODEC = simpleCodec(AshBlock::new);
	public static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 0, 63);

	private static final Identifier BREAK_ASH_BLOCK = ForestryConstants.forestry("break_ash_block");

	public AshBlock(Block.Properties properties) {
		super(properties.sound(SoundType.SAND).strength(0.6F).mapColor(MapColor.COLOR_LIGHT_GRAY));
	}

	@Override
	public MapCodec<AshBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AMOUNT);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}

	@Override
	public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		if (state.getValue(AMOUNT) > 0) {
			AdvancementHelper.tryUnlock(player, BREAK_ASH_BLOCK);
		}
	}

	@Override
	public int getDustColor(BlockState state, BlockGetter reader, BlockPos pos) {
		return state.getMapColor(reader, pos).col;
	}
}
