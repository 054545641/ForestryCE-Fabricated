package forestry.lepidopterology.cocoons;

import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.core.platform.util.ItemStackUtil;
import forestry.core.platform.util.SpeciesUtil;
import forestry.lepidopterology.cocoons.BlockCocoon;
import forestry.lepidopterology.features.LepidopterologyTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import forestry.core.platform.util.ValueIOUtil;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TileCocoon extends BlockEntity {
	private int maturationTime;
	private IButterfly caterpillar = SpeciesUtil.BUTTERFLY_TYPE.get().getDefaultSpecies().createIndividual();
	private boolean isSolid;

	public TileCocoon(BlockPos pos, BlockState state, boolean isSolid) {
		super(isSolid ? LepidopterologyTiles.SOLID_COCOON.tileType() : LepidopterologyTiles.COCOON.tileType(), pos, state);
		this.isSolid = isSolid;
	}

	/* SAVING & LOADING */
	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag compoundNBT = ValueIOUtil.toTag(input);
		HolderLookup.Provider registries = input.lookup();
		super.loadAdditional(input);

		if (compoundNBT.contains("Caterpillar")) {
            this.caterpillar = SpeciesUtil.deserializeIndividual(SpeciesUtil.BUTTERFLY_TYPE.get(), compoundNBT.getCompoundOrEmpty("Caterpillar"));
		}
		this.maturationTime = compoundNBT.getIntOr("CATMAT", 0);
		this.isSolid = compoundNBT.getBooleanOr("isSolid", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag compoundNBT = new CompoundTag();
		HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
		super.saveAdditional(output);

		Tag tag = SpeciesUtil.serializeIndividual(this.caterpillar);
		if (tag != null) {
			compoundNBT.put("Caterpillar", tag);
		}

		compoundNBT.putInt("CATMAT", this.maturationTime);
		compoundNBT.putBoolean("isSolid", this.isSolid);
		ValueIOUtil.mergeInto(output, compoundNBT);
	}

	public void onBlockTick() {
        this.maturationTime++;
		setChanged();

		IGenome caterpillarGenome = this.caterpillar.getGenome();
		int caterpillarMatureTime = Math
			.round((float) caterpillarGenome.getActiveValue(ButterflyChromosomes.LIFESPAN) / (caterpillarGenome.getActiveValue(ButterflyChromosomes.FERTILITY) * 2));

		if (this.maturationTime >= caterpillarMatureTime) {
			int age = getBlockState().getValue(BlockCocoon.AGE);
			if (age < 2) {
                this.maturationTime = 0;
				BlockState blockState = getBlockState().setValue(BlockCocoon.AGE, age + 1);
                this.level.setBlock(this.worldPosition, blockState, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
			} else if (this.caterpillar.canTakeFlight(this.level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ())) {
				List<ItemStack> cocoonDrops = this.caterpillar.getCocoonDrop(this.isSolid, this.caterpillar.getGenome().resolveActive(ButterflyChromosomes.COCOON));
				for (ItemStack drop : cocoonDrops) {
					ItemStackUtil.dropItemStackAsEntity(drop, this.level, this.worldPosition);
				}
                this.level.setBlockAndUpdate(getBlockPos(), Blocks.AIR.defaultBlockState());
				attemptButterflySpawn(this.level, this.caterpillar, getBlockPos());
			}
		}
	}

	private static void attemptButterflySpawn(Level world, IButterfly butterfly, BlockPos pos) {
		SpeciesUtil.BUTTERFLY_TYPE.get().spawnButterflyInWorld(world, butterfly.copy(), pos.getX(), pos.getY() + 0.1f, pos.getZ());
		//Forestry.LOGGER.trace("A caterpillar '{}' hatched at {}/{}/{}.", butterfly.getDisplayName(), pos.getX(), pos.getY(), pos.getZ());
	}

	public IButterfly getCaterpillar() {
		return this.caterpillar;
	}

	public void setCaterpillar(IButterfly caterpillar) {
		this.caterpillar = caterpillar;
		setChanged();
	}

	public List<ItemStack> getCocoonDrops() {
		return this.caterpillar.getCocoonDrop(this.isSolid, this.caterpillar.getGenome().resolveActive(ButterflyChromosomes.COCOON));
	}

	private HolderLookup.Provider getRegistries() {
		return this.level != null ? this.level.registryAccess() : RegistryAccess.EMPTY;
	}
}
