package forestry.core.platform.registration;

import net.minecraft.resources.Identifier;

public abstract class ModFeature implements IModFeature {
	protected final Identifier moduleId;
	protected final String name;

	protected ModFeature(Identifier moduleId, String name) {
		this.moduleId = moduleId;
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Identifier getModuleId() {
		return this.moduleId;
	}

	@Override
	public String getModId() {
		return ModFeatureRegistry.get(this.moduleId).getOwnerModId();
	}
}
