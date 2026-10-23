package forestry.lepidopterology.plugin;

import com.google.common.collect.ImmutableMap;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.lepidopterology.IButterflyCocoon;
import forestry.api.lepidopterology.IButterflyEffect;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.api.plugin.IButterflySpeciesBuilder;
import forestry.api.plugin.ILepidopterologyRegistration;
import forestry.apiimpl.plugin.Registrar;
import forestry.apiimpl.plugin.SpeciesRegistration;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

public class LepidopterologyRegistration extends SpeciesRegistration<IButterflySpeciesBuilder, IButterflySpecies, ButterflySpeciesBuilder> implements ILepidopterologyRegistration {
	private final Registrar<Identifier, IButterflyCocoon, IButterflyCocoon> cocoons = new Registrar<>(IButterflyCocoon.class);
	private final Registrar<Identifier, IButterflyEffect, IButterflyEffect> effects = new Registrar<>(IButterflyEffect.class);

	public LepidopterologyRegistration(ISpeciesType<IButterflySpecies, ?> type) {
		super(type);
	}

	@Override
	public IButterflySpeciesBuilder registerSpecies(Identifier id, String genus, String species, boolean dominant, TextColor serumColor, float rarity) {
		return register(id, genus, species)
			.setDominant(dominant)
			.setSerumColor(serumColor)
			.setRarity(rarity);
	}

	@Override
	public void registerCocoon(Identifier id, IButterflyCocoon cocoon) {
		this.cocoons.create(id, cocoon);
	}

	@Override
	public void registerEffect(Identifier id, IButterflyEffect effect) {
		this.effects.create(id, effect);
	}

	@Override
	protected ButterflySpeciesBuilder createSpeciesBuilder(Identifier id, String genus, String species) {
		return new ButterflySpeciesBuilder(id, genus, species);
	}

	public ImmutableMap<Identifier, IButterflyCocoon> getCocoons() {
		return this.cocoons.build();
	}

	public ImmutableMap<Identifier, IButterflyEffect> getEffects() {
		return this.effects.build();
	}
}
