package forestry.apiculture.bees.genetics;

import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.hives.IHiveDrop;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class HiveDrop implements IHiveDrop {
	private final Identifier speciesId;
	private final double chance;
	private final Supplier<List<ItemStack>> bonus;
	private final double ignobleChance;
	private final Map<IChromosome<?>, Allele<?>> alleles;

	public HiveDrop(double chance, Identifier speciesId, Supplier<List<ItemStack>> bonus, float ignobleChance, Map<IChromosome<?>, Allele<?>> alleles) {
		this.speciesId = speciesId;
		this.chance = chance;
		this.bonus = bonus;
		this.ignobleChance = ignobleChance;
		this.alleles = Map.copyOf(alleles);
	}

	@Override
	public IBee createIndividual(BlockGetter level, BlockPos pos) {
		// Species objects are replaced on reload; resolve the current one for each drop.
		return SpeciesUtil.getBeeSpecies(this.speciesId).createIndividual(this.alleles);
	}

	@Override
	public List<ItemStack> getExtraItems(BlockGetter level, BlockPos pos, int fortune) {
		ArrayList<ItemStack> result = new ArrayList<>();
		for (ItemStack stack : this.bonus.get()) {
			result.add(stack.copy());
		}

		return result;
	}

	@Override
	public double getChance(BlockGetter level, BlockPos pos, int fortune) {
		return this.chance;
	}

	@Override
	public double getIgnobleChance(BlockGetter level, BlockPos pos, int fortune) {
		return this.ignobleChance;
	}
}
