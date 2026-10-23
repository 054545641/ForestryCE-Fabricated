package forestry.api.arboriculture;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Provides access to tree-related registries, and to Forestry and Vanilla wood items.
 * Wood types are supplied by the arboriculture module as {@link IWoodType} implementations.
 * Forestry wood blocks have the same block state properties as vanilla ones.
 * Note that all doors are fireproof (even vanilla).
 *
 * @see WoodBlockKind
 * @since 2.6.0
 */
public interface ITreeManager {
	/**
	 * @param block The block to query the refractory waxed form of, ex. Oak Planks
	 * @return The resulting block after refractory wax is used on it, ex. Oak Planks (Fireproof),
	 * or {@code null} if refractory wax cannot be applied to the block.
	 */
	@Nullable
	Block getRefractoryWaxed(Block block);

	/**
	 * This getter will be replaced by separate methods in 1.22, as ICharcoalManager will be removed
	 */
	ICharcoalManager getCharcoalManager();

	ItemStack getStack(IWoodType woodType, WoodBlockKind kind, boolean fireproof);

	BlockState getBlock(IWoodType woodType, WoodBlockKind kind, boolean fireproof);

	TagKey<Block> getLogBlockTag(IWoodType kind, boolean fireproof);

	TagKey<Item> getLogItemTag(IWoodType kind, boolean fireproof);

	List<IWoodType> getRegisteredWoodTypes();

	/**
	 * Call this after item registry to register the blocks/items for your wood type.
	 *
	 * @param woodType      The type of wood, ex. Oak or Teak
	 * @param woodBlockKind The kind of wood block, ex. Planks or Fence
	 * @param fireproof     Whether this is for the fireproof variant of the wood block kind (ignored in the case of non-burnable wood blocks)
	 * @param blockState    The default block state of the Planks/Fence/etc. for the given wood type
	 * @param itemStack     Supplier for the item form of Planks/Fence/etc. for the given wood type
	 */
	void register(IWoodType woodType, WoodBlockKind woodBlockKind, boolean fireproof, BlockState blockState, Supplier<Item> itemStack);

	/**
	 * Call this after item registry to register the block tags used by your wood type.
	 *
	 * @param woodType    The type of wood, ex. Oak or Teak
	 * @param fireproof   Whether these tags are for the fireproof logs or the regular logs
	 * @param logBlockTag The block tag for logs of this wood type
	 * @param logItemTag  The item tag for logs of this wood type
	 */
	void registerLogTag(IWoodType woodType, boolean fireproof, TagKey<Block> logBlockTag, TagKey<Item> logItemTag);
}
