package forestry.arboriculture.trees;

import com.google.common.collect.ImmutableMap;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ITreeManager;
import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;
import forestry.arboriculture.wood.WoodAccess;

public class TreeManager implements ITreeManager {
	private final ImmutableMap<Block, Block> refractoryWaxables;
	private final ICharcoalManager charcoalManager;

	public TreeManager(ImmutableMap<Block, Block> refractoryWaxables, ICharcoalManager charcoalManager) {
		this.refractoryWaxables = refractoryWaxables;
		this.charcoalManager = charcoalManager;
	}

	@Nullable
	@Override
	public Block getRefractoryWaxed(Block block) {
		return this.refractoryWaxables.get(block);
	}

	@Override
	public ICharcoalManager getCharcoalManager() {
		return this.charcoalManager;
	}

	/* WOOD ACCESS */
	// todo only make accessible after wood access is populated

	@Override
	public ItemStack getStack(IWoodType woodType, WoodBlockKind kind, boolean fireproof) {
		return WoodAccess.INSTANCE.getStack(woodType, kind, fireproof);
	}

	@Override
	public BlockState getBlock(IWoodType woodType, WoodBlockKind kind, boolean fireproof) {
		return WoodAccess.INSTANCE.getBlock(woodType, kind, fireproof);
	}

	@Override
	public TagKey<Block> getLogBlockTag(IWoodType kind, boolean fireproof) {
		return WoodAccess.INSTANCE.getLogBlockTag(kind, fireproof);
	}

	@Override
	public TagKey<Item> getLogItemTag(IWoodType kind, boolean fireproof) {
		return WoodAccess.INSTANCE.getLogItemTag(kind, fireproof);
	}

	@Override
	public List<IWoodType> getRegisteredWoodTypes() {
		return WoodAccess.INSTANCE.getRegisteredWoodTypes();
	}

	@Override
	public void register(IWoodType woodType, WoodBlockKind woodBlockKind, boolean fireproof, BlockState blockState, Supplier<Item> itemStack) {
		WoodAccess.INSTANCE.register(woodType, woodBlockKind, fireproof, blockState, itemStack);
	}

	@Override
	public void registerLogTag(IWoodType woodType, boolean fireproof, TagKey<Block> logBlockTag, TagKey<Item> logItemTag) {
		WoodAccess.INSTANCE.registerLogTag(woodType, fireproof, logBlockTag, logItemTag);
	}
}
