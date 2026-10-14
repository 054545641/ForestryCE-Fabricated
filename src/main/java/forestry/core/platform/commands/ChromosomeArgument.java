package forestry.core.platform.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class ChromosomeArgument implements ISpeciesArgumentType<IChromosome<?>> {
	private final ISpeciesType<?, ?> type;
	private final HashMap<Identifier, IChromosome<?>> chromosomes;

	public ChromosomeArgument(ISpeciesType<?, ?> type) {
		this.type = type;

		ImmutableList<IChromosome<?>> chromosomes = type.getKaryotype().getChromosomes();
		this.chromosomes = new HashMap<>(chromosomes.size());

		for (IChromosome<?> chromosome : chromosomes) {
			this.chromosomes.put(chromosome.id(), chromosome);
		}
	}

	@Override
	public ISpeciesType<?, ?> type() {
		return this.type;
	}

	@Override
	public IChromosome<?> parse(StringReader reader) throws CommandSyntaxException {
		Identifier id = Identifier.read(reader);
		IChromosome<?> chromosome = this.chromosomes.get(id);

		if (chromosome == null) {
			throw LifeStageArgument.INVALID_VALUE.create(id);
		} else {
			return chromosome;
		}
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(this.chromosomes.keySet().stream().map(Identifier::toString), builder);
	}
}
