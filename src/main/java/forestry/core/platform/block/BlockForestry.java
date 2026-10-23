package forestry.core.platform.block;

import com.mojang.authlib.GameProfile;
import forestry.Forestry;
import forestry.core.platform.owner.IOwnedTile;
import forestry.core.platform.owner.IOwnerHandler;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.tile.TileUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BlockForestry extends Block {
	protected BlockForestry(Block.Properties properties) {
		this(properties, false);
	}

	protected BlockForestry(Block.Properties properties, boolean defaultStrength) {
		super(defaultStrength ? properties
			.strength(1.5f) : properties);
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (world.isClientSide()) {
			return;
		}

		if (placer instanceof Player) {
			TileUtil.actOnTile(world, pos, IOwnedTile.class, tile -> {
				IOwnerHandler ownerHandler = tile.getOwnerHandler();
				Player player = (Player) placer;
				GameProfile gameProfile = player.getGameProfile();
				ownerHandler.setOwner(gameProfile);
			});
		}
	}

	@Override
	public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, @javax.annotation.Nullable net.minecraft.world.level.redstone.Orientation orientation, boolean moved) {
		super.neighborChanged(state, world, pos, neighborBlock, orientation, moved);

		if (world instanceof Level) {
			try {
				TileUtil.actOnTile(world, pos, TileForestry.class, tile -> tile.onNeighborTileChange((Level) world, pos, pos));
			} catch (StackOverflowError error) {
				Forestry.LOGGER.error("Stack Overflow Error in BlockForestry.onNeighborChange()", error);
				throw error;
			}
		}
	}
}
