package forestry.agriculture.farmlogic.farmables;

import com.google.common.collect.ImmutableSet;
import forestry.api.arboriculture.ForestryFruits;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.ITreeSpeciesType;
import forestry.api.core.IProduct;
import forestry.api.agriculture.ICrop;
import forestry.api.agriculture.IFarmable;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.ForestryTags;
import forestry.core.platform.util.SpeciesUtil;
import forestry.agriculture.farmlogic.crops.CropDestroy;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class FarmableGE implements IFarmable {
	private final ImmutableSet<Item> windfall;

	public FarmableGE() {
		ImmutableSet.Builder<Item> builder = new ImmutableSet.Builder<>();
		for (ITreeSpecies species : SpeciesUtil.TREE_TYPE.get().getAllSpecies()) {
			var genome = species.getDefaultGenome();

			if (!genome.getActiveValue(TreeChromosomes.FRUIT).equals(ForestryFruits.NONE)) {
				IFruit fruit = genome.resolveActive(TreeChromosomes.FRUIT);
				for (IProduct product : fruit.getProducts()) {
					builder.add(product.item());
				}
			}
		}
		this.windfall = builder.build();
	}

	@Override
	public boolean isSaplingAt(Level level, BlockPos pos, BlockState state) {
		return state.is(ForestryTags.Blocks.TREE_SAPLINGS);
	}

	@Override
	@Nullable
	public ICrop getCropAt(Level level, BlockPos pos, BlockState state) {
		if (!state.is(BlockTags.LOGS)) {
			return null;
		}

		return new CropDestroy(level, state, pos, null);
	}

	@Override
	public boolean plantSaplingAt(Player player, ItemStack germling, Level level, BlockPos pos) {
		ITreeSpeciesType treeRoot = SpeciesUtil.TREE_TYPE.get();

		return IIndividualHandlerItem.filter(germling, individual -> individual instanceof ITree tree && treeRoot.plantSapling(level, tree, player.getGameProfile(), pos));
	}

	@Override
	public boolean isGermling(ItemStack stack) {
		return SpeciesUtil.TREE_TYPE.get().isMember(stack);
	}

	@Override
	public boolean isWindfall(ItemStack stack) {
		return this.windfall.contains(stack.getItem());
	}
}
