package forestry.core.content.backpacks.compat;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.util.JeiUtil;
import forestry.core.content.backpacks.features.BackpackItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class StorageJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.STORAGE;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		JeiUtil.addDescription(registration, "miner_bag",
			BackpackItems.MINER_BACKPACK,
			BackpackItems.MINER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, "digger_bag",
			BackpackItems.DIGGER_BACKPACK,
			BackpackItems.DIGGER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, "forester_bag",
			BackpackItems.FORESTER_BACKPACK,
			BackpackItems.FORESTER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, "hunter_bag",
			BackpackItems.HUNTER_BACKPACK,
			BackpackItems.HUNTER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, "adventurer_bag",
			BackpackItems.ADVENTURER_BACKPACK,
			BackpackItems.ADVENTURER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, "builder_bag",
			BackpackItems.BUILDER_BACKPACK,
			BackpackItems.BUILDER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, "brewer_bag",
			BackpackItems.BREWER_BACKPACK,
			BackpackItems.BREWER_BACKPACK_T_2
		);
		JeiUtil.addDescription(registration, BackpackItems.APIARIST_BACKPACK);
		JeiUtil.addDescription(registration, BackpackItems.LEPIDOPTERIST_BACKPACK);
	}
}
