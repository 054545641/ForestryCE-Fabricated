package forestry.arboriculture.leaves;

import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.arboriculture.genetics.ITree;

import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.core.platform.block.IColoredBlock;
import forestry.core.platform.util.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


import javax.annotation.Nullable;

public class DecorativeLeavesBlock extends Block implements IColoredBlock, forestry.core.platform.block.DirectionalFireSpread {
	private final ForestryLeafType type;

	public DecorativeLeavesBlock(Properties properties, ForestryLeafType type) {
		super(properties
			.strength(0.2f)
			.sound(SoundType.GRASS)
			.noOcclusion()
			.isValidSpawn(BlockUtil.IS_PARROT_OR_OCELOT)
			.isSuffocating(BlockUtil.ALWAYS)
			.isRedstoneConductor(BlockUtil.NEVER)
			.isViewBlocking(BlockUtil.NEVER));
		this.type = type;
	}

	public Identifier getSpeciesId() {
		return this.type.getSpeciesId();
	}

	public ForestryLeafType getType() {
		return this.type;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		if (this.type == ForestryLeafType.WILLOW) {
			return Shapes.empty();
		}
		return super.getCollisionShape(state, worldIn, pos, context);
	}

	@Override
	public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
		return Shapes.empty();
	}

	@Override
	public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn, net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean isPrecise) {
		Vec3 motion = entityIn.getDeltaMovement();
		entityIn.setDeltaMovement(motion.multiply(0.4D, 1.0D, 0.4D));
	}

	/* PROPERTIES */
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return 60;
	}

	public boolean isFlammable(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		return true;
	}

	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
		if (face == Direction.DOWN) {
			return 20;
		} else if (face != Direction.UP) {
			return 10;
		} else {
			return 5;
		}
	}

	@Override
	public int colorMultiplier(BlockState state, @Nullable BlockGetter level, @Nullable BlockPos pos, int tintIndex) {
		ITree individual = this.type.getIndividual();

		if (tintIndex == AbstractLeavesBlock.FRUIT_COLOR_INDEX) {
			IFruit fruitProvider = individual.getGenome().resolveActive(TreeChromosomes.FRUIT);
			return fruitProvider.getDecorativeColor();
		}
		return forestry.core.platform.client.ClientsideCode.getLeafTint(individual.getSpecies(), level, pos);
	}
}
