package forestry.core.data.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import forestry.api.core.HumidityType;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.IMutationCondition;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.engine.genetics.mutations.MutationConditionBiome;
import forestry.core.engine.genetics.mutations.MutationConditionCave;
import forestry.core.engine.genetics.mutations.MutationConditionDaytime;
import forestry.core.engine.genetics.mutations.MutationConditionHumidity;
import forestry.core.engine.genetics.mutations.MutationConditionRequiresResource;
import forestry.core.engine.genetics.mutations.MutationConditionTemperature;
import forestry.core.engine.genetics.mutations.MutationConditionTimeLimited;
import forestry.core.engine.genetics.mutations.MutationRecipe;

/**
 * Datagen builder for a single {@link MutationRecipe}. Mirrors the old {@code IMutationBuilder} method surface so the
 * built-in mutations can be ported one-to-one.
 */
public class MutationRecipeBuilder {
	private final Identifier speciesTypeId;
	private final Identifier firstParentId;
	private final Identifier secondParentId;
	private final Identifier resultId;
	private final float chance;
	private final List<IMutationCondition> conditions = new ArrayList<>();
	private final Map<Identifier, Allele<?>> resultAlleles = new LinkedHashMap<>();

	public MutationRecipeBuilder(Identifier speciesTypeId, Identifier firstParentId, Identifier secondParentId, Identifier resultId, float chance) {
		this.speciesTypeId = speciesTypeId;
		this.firstParentId = firstParentId;
		this.secondParentId = secondParentId;
		this.resultId = resultId;
		this.chance = chance;
	}

	public Identifier getSpeciesTypeId() {
		return this.speciesTypeId;
	}

	public Identifier getResultId() {
		return this.resultId;
	}

	public MutationRecipeBuilder temperature(TemperatureType temperature) {
		return temperature(temperature, temperature);
	}

	public MutationRecipeBuilder temperature(TemperatureType min, TemperatureType max) {
		this.conditions.add(new MutationConditionTemperature(min, max));
		return this;
	}

	public MutationRecipeBuilder humidity(HumidityType humidity) {
		return humidity(humidity, humidity);
	}

	public MutationRecipeBuilder humidity(HumidityType min, HumidityType max) {
		this.conditions.add(new MutationConditionHumidity(min, max));
		return this;
	}

	public MutationRecipeBuilder biome(TagKey<Biome> biome) {
		this.conditions.add(new MutationConditionBiome(biome));
		return this;
	}

	public MutationRecipeBuilder dateRange(int startMonth, int startDay, int endMonth, int endDay) {
		this.conditions.add(new MutationConditionTimeLimited(startMonth, startDay, endMonth, endDay));
		return this;
	}

	public MutationRecipeBuilder day() {
		this.conditions.add(new MutationConditionDaytime(true));
		return this;
	}

	public MutationRecipeBuilder night() {
		this.conditions.add(new MutationConditionDaytime(false));
		return this;
	}

	public MutationRecipeBuilder requiresResource(BlockState... acceptedBlockStates) {
		this.conditions.add(new MutationConditionRequiresResource(acceptedBlockStates));
		return this;
	}

	public MutationRecipeBuilder cave() {
		this.conditions.add(new MutationConditionCave());
		return this;
	}

	public MutationRecipeBuilder condition(IMutationCondition condition) {
		this.conditions.add(condition);
		return this;
	}

	public <V> MutationRecipeBuilder resultAllele(IChromosome<V> chromosome, Allele<V> allele) {
		this.resultAlleles.put(chromosome.id(), allele);
		return this;
	}

	public MutationRecipe build(Identifier id) {
		return new MutationRecipe(this.speciesTypeId, id, this.firstParentId, this.secondParentId, this.resultId, this.chance, List.copyOf(this.conditions), Map.copyOf(this.resultAlleles));
	}
}
