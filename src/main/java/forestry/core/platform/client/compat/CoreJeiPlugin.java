package forestry.core.platform.client.compat;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.util.JeiUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class CoreJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.CORE;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		JeiUtil.addDescription(registration, JeiUtil.item("compost"));
		JeiUtil.addDescription(registration, JeiUtil.item("mulch"));
		JeiUtil.addDescription(registration, JeiUtil.item("fertilizer"));
		// Explains that the Phosphor Torch is waterloggable (for.jei.description.phosphor_torch)
		JeiUtil.addDescription(registration, JeiUtil.item("phosphor_torch"));
	}
}
