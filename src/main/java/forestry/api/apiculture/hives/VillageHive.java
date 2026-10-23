package forestry.api.apiculture.hives;

import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import net.minecraft.resources.Identifier;

import java.util.Map;

public record VillageHive(Identifier speciesId, Map<IChromosome<?>, Allele<?>> alleles) {
}
