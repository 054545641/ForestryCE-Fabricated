package forestry.arboriculture.sapling;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.core.genetics.IBreedingTracker;
import forestry.arboriculture.features.ArboricultureTiles;
import forestry.arboriculture.worldgen.AbstractArboricultureFeature;
import forestry.core.platform.owner.IOwnedTile;
import forestry.core.platform.owner.IOwnerHandler;
import forestry.core.platform.owner.OwnerHandler;
import forestry.core.platform.util.SpeciesUtil;
import forestry.core.platform.worldgen.AbstractForestryFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

import javax.annotation.Nonnull;
import java.util.Optional;
import forestry.arboriculture.trees.TileTreeContainer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;

public class SaplingBlockEntity extends TileTreeContainer implements IOwnedTile {
	public static final ModelProperty<ITreeSpecies> TREE_SPECIES = new ModelProperty<>();

	private final OwnerHandler ownerHandler = new OwnerHandler();

	private int timesTicked = 0;

	public SaplingBlockEntity(BlockPos pos, BlockState state) {
		super(ArboricultureTiles.SAPLING.tileType(), pos, state);
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

        this.timesTicked = nbt.getIntOr("TT", 0);
		this.ownerHandler.read(nbt, registries);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		nbt.putInt("TT", this.timesTicked);
		this.ownerHandler.write(nbt, registries);
	
		ValueIOUtil.mergeInto(output, nbt);
	}

	@Override
	public void onBlockTick(Level worldIn, BlockPos pos, BlockState state, RandomSource rand) {
        this.timesTicked++;
		setChanged();
		tryGrow(rand, false);
	}

	private static int getRequiredMaturity(Level world, ITree tree) {
		//ITreekeepingMode treekeepingMode = SpeciesUtil.TREE_TYPE.get().getTreekeepingMode(world);
		//float maturationModifier = treekeepingMode.getMaturationModifier(tree.getGenome(), 1f);
		return tree.getRequiredMaturity();//Math.round(tree.getRequiredMaturity() * maturationModifier);
	}

	public boolean canAcceptBoneMeal(RandomSource rand) {
		ITree tree = getTree();

		if (tree == null) {
			return false;
		}

		int maturity = getRequiredMaturity(this.level, tree);
		if (this.timesTicked < maturity) {
			return true;
		}

		Feature<NoneFeatureConfiguration> generator = tree.getTreeGenerator((ServerLevel) this.level, getBlockPos(), true);
		if (generator instanceof AbstractArboricultureFeature arboricultureGenerator) {
			arboricultureGenerator.preGenerate(getTree().getGenome(), this.level, rand, getBlockPos());
			return arboricultureGenerator.getValidGrowthPos(this.level, getBlockPos()) != null;
		} else {
			return true;
		}
	}

	public void tryGrow(RandomSource random, boolean boneMealed) {
		ITree tree = getTree();

		if (tree == null) {
			return;
		}

		int maturity = getRequiredMaturity(this.level, tree);
		if (this.timesTicked < maturity) {
			if (boneMealed) {
                this.timesTicked = maturity;
				setChanged();
			}
			return;
		}

		Feature<NoneFeatureConfiguration> generator = tree.getTreeGenerator((ServerLevel) this.level, getBlockPos(), boneMealed);
		final boolean generated;
		if (generator instanceof AbstractForestryFeature base) {
			generated = base.place(tree.getGenome(), this.level, random, getBlockPos(), false);
		} else {
			ServerLevel level = (ServerLevel) this.level;
			generated = generator.place(new FeaturePlaceContext<>(Optional.empty(), level, level.getChunkSource().getGenerator(), random, getBlockPos(), FeatureConfiguration.NONE));
		}

		if (generated) {
			IBreedingTracker breedingTracker = SpeciesUtil.TREE_TYPE.get().getBreedingTracker(this.level, getOwnerHandler().getOwner());
			breedingTracker.registerBirth(tree.getSpecies());
		}
	}

	@Nonnull
	@Override
	public ModelData getModelData() {
		ITree tree = getTree();
		if (tree == null) {
			return ModelData.EMPTY;
		}
		return ModelData.builder().with(TREE_SPECIES, tree.getSpecies()).build();
	}

	@Override
	public IOwnerHandler getOwnerHandler() {
		return this.ownerHandler;
	}
}
