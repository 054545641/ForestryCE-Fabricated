package forestry.apiimpl.client;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.Identifier;

import forestry.api.client.lepidopterology.IButterflyClientManager;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpecies;

public class ButterflyClientManager implements IButterflyClientManager {
	private final Map<Identifier, Pair<Identifier, Identifier>> textures;

	public ButterflyClientManager(Map<Identifier, Pair<Identifier, Identifier>> textures) {
		this.textures = textures;
	}

	@Override
	public Pair<Identifier, Identifier> getTextures(IButterflySpecies species) {
		Identifier id = species.id();
		Pair<Identifier, Identifier> pair = this.textures.get(id);
		if (pair != null) {
			return pair;
		}
		// Default item/entity texture naming convention, computed lazily from the species id itself (mirrors
		// TreeClientManager#getTint's render-time fallback), so it works for datapack-added species too and needs no
		// species-list iteration at client-registration time.
		return defaultTexturesFor(id);
	}

	@Override
	public Pair<Identifier, Identifier> getDefaultTextures() {
		Pair<Identifier, Identifier> cabbageWhite = this.textures.get(ForestryButterflySpecies.CABBAGE_WHITE);
		if (cabbageWhite != null) {
			return cabbageWhite;
		}
		// last-resort literal fallback so bake never NPEs before any species/textures are registered
		return defaultTexturesFor(ForestryButterflySpecies.CABBAGE_WHITE);
	}

	@Override
	public Collection<Pair<Identifier, Identifier>> getAllTextures() {
		return Collections.unmodifiableCollection(this.textures.values());
	}

	private static Pair<Identifier, Identifier> defaultTexturesFor(Identifier id) {
		String path = id.getPath().replace("butterfly_", "");
		return Pair.of(
			Identifier.fromNamespaceAndPath(id.getNamespace(), "item/butterfly/" + path),
			Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/entity/butterfly/" + path + ".png")
		);
	}
}
