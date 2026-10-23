package forestry.api.plugin;

import forestry.api.lepidopterology.IButterflyCocoon;
import forestry.api.lepidopterology.IButterflyEffect;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

public interface ILepidopterologyRegistration {
	/**
	 * Register a new butterfly species.
	 *
	 * @param id         The unique ID for this species.
	 * @param genus      The scientific name of the genus containing this species. See {@link forestry.api.core.genetics.ForestryTaxa}.
	 * @param species    The scientific name of the species without the genus.
	 * @param dominant   Whether this species appears as a dominant allele in the genome.
	 * @param serumColor The color of this butterfly's serum.
	 * @param rarity     The rarity of this species for spawning.
	 */
	IButterflySpeciesBuilder registerSpecies(Identifier id, String genus, String species, boolean dominant, TextColor serumColor, float rarity);

	void registerCocoon(Identifier id, IButterflyCocoon cocoon);

	void registerEffect(Identifier id, IButterflyEffect effect);
}
