package forestry.arboriculture.leaves;

import com.mojang.authlib.GameProfile;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.TreeLifeStage;

import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Genetic leaves with no tile entity, used for worldgen trees.
 * Similar to decorative leaves, but these will drop saplings and can be used for pollination.
 */
public class DefaultLeavesBlock extends AbstractLeavesBlock implements ILeafTypeBlock {
	private final ForestryLeafType type;

	public DefaultLeavesBlock(Block.Properties properties, ForestryLeafType type) {
		super(properties);
		this.type = type;
	}

	public Identifier getSpeciesId() {
		return this.type.getSpeciesId();
	}

	public ForestryLeafType getType() {
		return this.type;
	}

	@Override
	protected void getLeafDrop(List<ItemStack> drops, Level level, @Nullable BlockPos pos, @Nullable GameProfile profile, float saplingModifier, int fortune, LootParams.Builder context) {
		ITree tree = this.type.getIndividual();

		// Add saplings
		List<ITree> saplings = tree.getSaplings(level, pos, profile, saplingModifier);
		for (ITree sapling : saplings) {
			if (sapling != null) {
				drops.add(SpeciesUtil.TREE_TYPE.get().createStack(sapling, TreeLifeStage.SAPLING));
			}
		}
	}

	@Override
	protected ITree getTree(BlockGetter world, BlockPos pos) {
		return this.type.getIndividual();
	}

	@Override
	public int colorMultiplier(BlockState state, @Nullable BlockGetter level, @Nullable BlockPos pos, int tintIndex) {
		return forestry.core.platform.client.ClientsideCode.getLeafTint(this.type.getIndividual().getSpecies(), level, pos);
	}
}
