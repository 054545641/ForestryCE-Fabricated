package forestry.apiculture.client;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.core.genetics.IGenome;
import forestry.apiculture.particles.BeeParticleRender;
import net.minecraft.core.BlockPos;
import java.util.List;

import forestry.api.client.apiculture.IBeeClientManager;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

public class BeeClientManager implements IBeeClientManager {
	// life stage -> default model location
	private final IdentityHashMap<ILifeStage, Identifier> defaultModels;
	// life stage -> (species id -> custom model location)
	private final IdentityHashMap<ILifeStage, Map<Identifier, Identifier>> customModels;

	public BeeClientManager(IdentityHashMap<ILifeStage, Identifier> defaultModels, IdentityHashMap<ILifeStage, Map<Identifier, Identifier>> customModels) {
		this.defaultModels = defaultModels;
		this.customModels = customModels;
	}

	@Override
	public Identifier getModelLocation(ILifeStage stage, Identifier speciesId) {
		Map<Identifier, Identifier> customs = this.customModels.get(stage);
		Identifier custom = customs == null ? null : customs.get(speciesId);

		if (custom != null) {
			return custom;
		}

		return Objects.requireNonNull(this.defaultModels.get(stage), "No default bee model registered for life stage " + stage.getSerializedName());
	}

	@Override
	public Identifier getDefaultModelLocation(ILifeStage stage) {
		return Objects.requireNonNull(this.defaultModels.get(stage), "No default bee model registered for life stage " + stage.getSerializedName());
	}

	@Override
	public Collection<Identifier> getAllModelLocations(ILifeStage stage) {
		LinkedHashSet<Identifier> locations = new LinkedHashSet<>();
		Identifier defaultModel = this.defaultModels.get(stage);

		if (defaultModel != null) {
			locations.add(defaultModel);
		}

		Map<Identifier, Identifier> customs = this.customModels.get(stage);

		if (customs != null) {
			locations.addAll(customs.values());
		}

		return locations;
	}

	@Override
	public void addBeeHiveParticles(IBeeHousing housing, IGenome genome, List<BlockPos> flowerPositions) {
		BeeParticleRender.addBeeHiveFX(housing, genome, flowerPositions);
	}
}
