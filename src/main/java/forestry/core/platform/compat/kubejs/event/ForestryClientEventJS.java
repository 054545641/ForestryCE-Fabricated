package forestry.core.platform.compat.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.client.arboriculture.ILeafTint;
import forestry.api.client.plugin.IClientRegistration;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.resources.Identifier;

public class ForestryClientEventJS implements KubeEvent {
	private final IClientRegistration wrapped;

	public ForestryClientEventJS(IClientRegistration wrapped) {
		this.wrapped = wrapped;
	}

	public void setDefaultBeeModel(ILifeStage stage, Identifier modelLocation) {
		this.wrapped.setDefaultBeeModel(stage, modelLocation);
	}

	public void setCustomBeeModel(Identifier speciesId, ILifeStage stage, Identifier model) {
		this.wrapped.setCustomBeeModel(speciesId, stage, model);
	}

	public void setSaplingModel(Identifier speciesId, Identifier blockModel, Identifier itemModel) {
		this.wrapped.setSaplingModel(speciesId, blockModel, itemModel);
	}

	public void setLeafSprite(Identifier speciesId, ILeafSprite sprite) {
		this.wrapped.setLeafSprite(speciesId, sprite);
	}

	public void setLeafTint(Identifier speciesId, ILeafTint tint) {
		this.wrapped.setLeafTint(speciesId, tint);
	}

	public void setButterflySprites(Identifier speciesId, Identifier itemTexture, Identifier entityTexture) {
		this.wrapped.setButterflySprites(speciesId, itemTexture, entityTexture);
	}
}
