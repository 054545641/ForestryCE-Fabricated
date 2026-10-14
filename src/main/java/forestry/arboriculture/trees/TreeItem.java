package forestry.arboriculture.trees;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.core.machines.IVariableFermentable;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.engine.genetics.ItemGE;
import forestry.core.platform.item.ITintedItem;
import forestry.core.platform.util.BlockUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;

public class TreeItem extends ItemGE implements IVariableFermentable, ITintedItem {
	public TreeItem(TreeLifeStage type, Properties properties) {
		super(properties, type);
	}

	@Override
	protected ITreeSpecies getSpecies(ItemStack stack) {
		return IIndividualHandlerItem.getSpecies(stack, SpeciesUtil.TREE_TYPE.get());
	}

	@Override
	public ISpeciesType<?, ?> getType() {
		return SpeciesUtil.TREE_TYPE.get();
	}

	@Override
	public int getColorFromItemStack(ItemStack itemstack, int renderPass) {
		return getSpecies(itemstack).getGermlingColor(this.stage, renderPass);
	}

	@Override
	public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
		BlockHitResult traceResult = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.ANY);
		ItemStack stack = playerIn.getItemInHand(handIn);

		if (traceResult.getType() == HitResult.Type.BLOCK) {
			IIndividual tree = IIndividualHandlerItem.getIndividual(stack);

			if (tree != null) {
				BlockPos pos = traceResult.getBlockPos();

				if (this.stage == TreeLifeStage.SAPLING) {
					BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(playerIn, handIn, traceResult));

					return onItemRightClickSapling(stack, worldIn, playerIn, pos, (ITree) tree, context);
				} else if (this.stage == TreeLifeStage.POLLEN) {
					return onItemRightClickPollen(stack, worldIn, playerIn, pos, (ITree) tree);
				}
			}
		}

		return InteractionResult.PASS;
	}

	private static InteractionResult onItemRightClickPollen(ItemStack stack, Level level, Player player, BlockPos pos, ITree pollen) {
		if (!TreeUtil.canMate(TreeUtil.getTreeSafe(level, pos), pollen)) {
			return InteractionResult.PASS;
		}

		LeavesBlockEntity leaves = TreeUtil.getOrCreateLeaves(level, pos, true);
		if (leaves == null || !TreeUtil.canMate(leaves.getTree(), pollen)) {
			return InteractionResult.PASS;
		}

		if (!level.isClientSide()) {
			leaves.setMate(pollen);

			BlockUtil.sendDestroyEffects(level, pos, level.getBlockState(pos));

			if (!player.isCreative()) {
				stack.shrink(1);
			}

			return InteractionResult.CONSUME;
		}

		return InteractionResult.SUCCESS;
	}

	private static InteractionResult onItemRightClickSapling(ItemStack stack, Level worldIn, Player player, BlockPos pos, ITree tree, BlockPlaceContext context) {
		// x, y, z are the coordinates of the block "hit", can thus either be the soil or tall grass, etc.
		BlockState hitBlock = worldIn.getBlockState(pos);
		if (!hitBlock.canBeReplaced(context)) {
			pos = context.getClickedPos();
			if (!worldIn.getBlockState(pos).canBeReplaced(context)) {
				return InteractionResult.PASS;
			}
		}

		if (tree.canStay(worldIn, pos)) {
			if (SpeciesUtil.TREE_TYPE.get().plantSapling(worldIn, tree, player.getGameProfile(), pos)) {
				if (!player.isCreative()) {
					stack.shrink(1);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public float getFermentationModifier(ItemStack stack) {
		IIndividual tree = IIndividualHandlerItem.getIndividual(stack);
		if (tree == null) {
			return 1.0f;
		}
		return tree.getGenome().getActiveValue(TreeChromosomes.SAPPINESS) * 10;
	}

	@Override
	public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType, net.minecraft.world.level.block.entity.FuelValues fuelValues) {
		return 100;
	}
}
