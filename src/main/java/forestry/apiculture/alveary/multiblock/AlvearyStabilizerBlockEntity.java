package forestry.apiculture.alveary.multiblock;

import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.IBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.multiblock.IAlvearyComponent;
import forestry.apiculture.alveary.AlvearyBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AlvearyStabilizerBlockEntity extends AbstractAlvearyBlockEntity implements IAlvearyComponent.BeeModifier<AlvearyMultiblockLogic> {
	private static final IBeeModifier MODIFIER = new IBeeModifier() {
		@Override
		public float modifyMutationChance(IGenome genome, IGenome mate, IMutation<IBeeSpecies> mutation, float currentChance) {
			return 0.0f;
		}
	};

	public AlvearyStabilizerBlockEntity(BlockPos pos, BlockState state) {
		super(AlvearyBlock.Type.STABILIZER, pos, state);
	}

	@Override
	public IBeeModifier getBeeModifier() {
		return MODIFIER;
	}
}
