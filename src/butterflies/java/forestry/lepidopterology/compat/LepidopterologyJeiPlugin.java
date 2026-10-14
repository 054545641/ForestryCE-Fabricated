package forestry.lepidopterology.compat;

import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.util.JeiUtil;
import forestry.core.platform.util.SpeciesUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class LepidopterologyJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.LEPIDOPTEROLOGY;
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registry) {
		JeiUtil.registerItemSubtypes(registry, ButterflyChromosomes.SPECIES, SpeciesUtil.BUTTERFLY_TYPE.get());
	}
}
