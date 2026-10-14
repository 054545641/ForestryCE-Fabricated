package forestry.apiimpl.client.plugin;

import com.mojang.datafixers.util.Pair;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.genetics.IAnalyzerPlugin;
import forestry.api.client.plugin.IClientRegistration;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ClientRegistration implements IClientRegistration {
	// ID -> (life stage -> bee model)
	private final IdentityHashMap<ILifeStage, Map<Identifier, Identifier>> beeModels = new IdentityHashMap<>();
	// life stage -> bee model
	private final IdentityHashMap<ILifeStage, Identifier> defaultBeeModels = new IdentityHashMap<>();
	// ID -> (butterfly item texture, entity texture)
	private final HashMap<Identifier, Pair<Identifier, Identifier>> butterflyTextures = new HashMap<>();
	// ID -> (sapling block model, item model)
	private final HashMap<Identifier, Pair<Identifier, Identifier>> saplingModels = new HashMap<>();
	// ID -> leaf sprite
	private final HashMap<Identifier, ILeafSprite> leafSprites = new HashMap<>();
	// ID -> leaf tints
	private final HashMap<Identifier, ILeafTint> leafTints = new HashMap<>();
	// ID -> Analyzer plugin
	private final HashMap<Identifier, IAnalyzerPlugin<?, ?>> analyzerPlugins = new HashMap<>();

	@Override
	public void setDefaultBeeModel(ILifeStage stage, Identifier modelLocation) {
		this.defaultBeeModels.put(stage, modelLocation);
	}

	@Override
	public void setCustomBeeModel(Identifier speciesId, ILifeStage stage, Identifier model) {
		this.beeModels.computeIfAbsent(stage, k -> new HashMap<>()).put(speciesId, model);
	}

	@Override
	public void setSaplingModel(Identifier speciesId, Identifier blockModel, Identifier itemModel) {
		this.saplingModels.put(speciesId, Pair.of(blockModel, itemModel));
	}

	@Override
	public void setLeafSprite(Identifier speciesId, ILeafSprite sprite) {
		this.leafSprites.put(speciesId, sprite);
	}

	@Override
	public void setLeafTint(Identifier speciesId, ILeafTint tint) {
		this.leafTints.put(speciesId, tint);
	}

	@Override
	public void setButterflySprites(Identifier speciesId, Identifier itemTexture, Identifier entityTexture) {
		this.butterflyTextures.put(speciesId, Pair.of(itemTexture, entityTexture));
	}

	@Override
	public void setAnalyzerPlugin(Identifier speciesTypeId, IAnalyzerPlugin<?, ?> plugin) {
		this.analyzerPlugins.put(speciesTypeId, plugin);
	}

	public Map<ILifeStage, Map<Identifier, Identifier>> getBeeModels() {
		return this.beeModels;
	}

	public HashMap<Identifier, Pair<Identifier, Identifier>> getSaplingModels() {
		return this.saplingModels;
	}

	public HashMap<Identifier, ILeafSprite> getLeafSprites() {
		return this.leafSprites;
	}

	public HashMap<Identifier, ILeafTint> getTints() {
		return this.leafTints;
	}

	public HashMap<Identifier, Pair<Identifier, Identifier>> getButterflyTextures() {
		return this.butterflyTextures;
	}

	public Identifier getDefaultBeeModel(ILifeStage stage) {
		return this.defaultBeeModels.get(stage);
	}

	public HashMap<Identifier, IAnalyzerPlugin<?, ?>> getAnalyzerPlugins() {
		return this.analyzerPlugins;
	}
}
