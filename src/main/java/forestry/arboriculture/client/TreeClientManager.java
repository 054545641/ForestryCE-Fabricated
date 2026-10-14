package forestry.arboriculture.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.arboriculture.ITreeClientManager;

public class TreeClientManager implements ITreeClientManager {
	private final Map<Identifier, ILeafSprite> sprites;
	private final Map<Identifier, ILeafTint> tints;
	private final Map<Identifier, Pair<Identifier, Identifier>> models;

	public TreeClientManager(Map<Identifier, ILeafSprite> sprites, Map<Identifier, ILeafTint> tints, Map<Identifier, Pair<Identifier, Identifier>> models) {
		this.sprites = sprites;
		this.tints = tints;
		this.models = models;
	}

	@Override
	public ILeafSprite getLeafSprite(@Nullable ITreeSpecies species) {
		return species == null ? null : this.sprites.get(species.id());
	}

	@Override
	public Collection<ILeafSprite> getAllLeafSprites() {
		return new HashSet<>(this.sprites.values());
	}

	@Override
	public ILeafTint getTint(@Nullable ITreeSpecies species) {
		if (species == null) {
			return ILeafTint.DEFAULT;
		}
		// Resolve the escritoire-color fallback lazily from the species passed at render time, so it works for
		// datapack-added species too and needs no species-list iteration at client-registration time (the historical
		// per-species loop pre-seeded FixedLeafTint(escritoireColor); doing it here preserves that behavior reloadably).
		ILeafTint tint = this.tints.get(species.id());
		return tint != null ? tint : new FixedLeafTint(species.getEscritoireColor());
	}

	@Override
	public Pair<Identifier, Identifier> getSaplingModels(ITreeSpecies species) {
		Pair<Identifier, Identifier> pair = this.models.get(species.id());
		return pair != null ? pair : getDefaultSaplingModels();
	}

	@Override
	public Pair<Identifier, Identifier> getDefaultSaplingModels() {
		Pair<Identifier, Identifier> oak = this.models.get(ForestryTreeSpecies.OAK);
		if (oak != null) {
			return oak;
		}
		// last-resort literal fallback so bake never NPEs before any species/models are registered
		return Pair.of(Identifier.fromNamespaceAndPath("forestry", "block/oak_sapling"), Identifier.fromNamespaceAndPath("forestry", "item/oak_sapling"));
	}

	@Override
	public Collection<Pair<Identifier, Identifier>> getAllSaplingModels() {
		return Collections.unmodifiableCollection(this.models.values());
	}
}
