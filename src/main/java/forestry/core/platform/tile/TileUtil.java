package forestry.core.platform.tile;

import forestry.core.platform.util.InventoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import forestry.core.platform.inventory.InventoryTransferView;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class TileUtil {
	public static boolean isUsableByPlayer(Player player, BlockEntity tile) {
		BlockPos pos = tile.getBlockPos();
		Level world = tile.getLevel();

		return !tile.isRemoved() &&
			getTile(world, pos) == tile &&
			player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
	}

	/**
	 * Returns the tile at the specified position, returns null if it is the wrong type or does not exist.
	 * Avoids creating new tile entities when using a ChunkCache (off the main thread).
	 */
	@Nullable
	public static BlockEntity getTile(BlockGetter level, BlockPos pos) {
		if (level instanceof net.minecraft.server.level.ServerLevel server) {
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            return chunk == null ? null : chunk.getBlockEntity(pos);
        }
		if (level instanceof Level actual && !actual.hasChunkAt(pos)) return null;
		return level.getBlockEntity(pos);
	}

	/**
	 * Returns the tile of the specified class, returns null if it is the wrong type or does not exist.
	 * Avoids creating new tile entities when using a ChunkCache (off the main thread).
	 */
	@Nullable
	public static <T> T getTile(BlockGetter world, BlockPos pos, Class<T> tileClass) {
		BlockEntity tileEntity = getTile(world, pos);
		if (tileClass.isInstance(tileEntity)) {
			return tileClass.cast(tileEntity);
		} else {
			return null;
		}
	}

	@Nullable
	public static <T> T getTile(BlockEntity tileEntity, Class<T> tileClass) {
		if (tileClass.isInstance(tileEntity)) {
			return tileClass.cast(tileEntity);
		} else {
			return null;
		}
	}

	public static <T> void actOnTile(LevelReader world, BlockPos pos, Class<T> tileClass, Consumer<T> tileAction) {
		T tile = getTile(world, pos, tileClass);
		if (tile != null) {
			tileAction.accept(tile);
		}
	}

	@Nullable
	public static Storage<ItemVariant> getInventoryFromTile(@Nullable BlockEntity tile, @Nullable Direction side) {
		if (tile == null) {
			return null;
		}

		Level level = tile.getLevel();
		if (level != null) {
			Storage<ItemVariant> itemCap = ItemStorage.SIDED.find(level, tile.getBlockPos(), side);
			if (itemCap != null) {
				return itemCap;
			}
		}

		if (tile instanceof WorldlyContainer worldly) {
			return InventoryTransferView.automation(worldly, side);
		}

		if (tile instanceof Container container) {
			return InventoryTransferView.automation(container, side);
		}

		return null;
	}

	public static <T, C> Optional<T> getInterface(Level world, BlockPos pos, BlockApiLookup<T, C> capability, @Nullable C context) {
		return Optional.ofNullable(world.hasChunkAt(pos) ? capability.find(world, pos, context) : null);
	}

    public static ListTag saveItemsToList(HolderLookup.Provider registries, ItemStack[] offspring) {
        ListTag listTag = new ListTag();

        for (int i = 0; i < offspring.length; i++) {
            if (offspring[i] != null) {
                CompoundTag products = InventoryData.writeStack(offspring[i], registries);
                products.putByte("Slot", (byte) i);
                listTag.add(products);
            }
        }

        return listTag;
    }
}
