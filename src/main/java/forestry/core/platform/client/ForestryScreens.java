package forestry.core.platform.client;

import forestry.apiculture.alveary.*;
import forestry.apiculture.bees.BeeHousingScreen;
import forestry.apiculture.features.ApicultureMenuTypes;
import forestry.core.content.analyzer.GuiAnalyzer;
import forestry.core.content.analyzer.PortableAnalyzerScreen;
import forestry.core.content.backpacks.features.BackpackMenuTypes;
import forestry.core.content.backpacks.gui.GuiBackpack;
import forestry.core.content.burnbarrel.GuiBurnBarrel;
import forestry.core.content.energy.features.EnergyMenus;
import forestry.core.content.energy.screen.*;
import forestry.core.content.escritoire.GuiEscritoire;
import forestry.core.content.machines.features.FactoryMenuTypes;
import forestry.core.content.machines.gui.*;
import forestry.core.content.sorting.features.SortingMenuTypes;
import forestry.core.content.sorting.gui.GuiGeneticFilter;
import forestry.core.content.worktable.features.WorktableMenus;
import forestry.core.content.worktable.screens.WorktableScreen;
import forestry.core.engine.circuits.GuiSolderingIron;
import forestry.core.features.CoreMenuTypes;
import forestry.core.platform.gui.GuiNaturalistInventory;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ForestryScreens {
    public static void initialize() {
        MenuScreens.register(CoreMenuTypes.ALYZER.menuType(), PortableAnalyzerScreen::new);
        MenuScreens.register(CoreMenuTypes.ANALYZER.menuType(), GuiAnalyzer::new);
        MenuScreens.register(CoreMenuTypes.NATURALIST_INVENTORY.menuType(), GuiNaturalistInventory<forestry.core.platform.gui.ContainerNaturalistInventory>::new);
        MenuScreens.register(CoreMenuTypes.ESCRITOIRE.menuType(), GuiEscritoire::new);
        MenuScreens.register(CoreMenuTypes.BURN_BARREL.menuType(), GuiBurnBarrel::new);
        MenuScreens.register(CoreMenuTypes.SOLDERING_IRON.menuType(), GuiSolderingIron::new);
        MenuScreens.register(BackpackMenuTypes.BACKPACK.menuType(), GuiBackpack::new);
        MenuScreens.register(BackpackMenuTypes.NATURALIST_BACKPACK.menuType(), GuiNaturalistInventory<forestry.core.content.backpacks.gui.ContainerNaturalistBackpack>::new);
        MenuScreens.register(EnergyMenus.ENGINE_BIOGAS.menuType(), BiogasEngineScreen::new);
        MenuScreens.register(EnergyMenus.ENGINE_COMBUSTION.menuType(), CombustionEngineScreen::new);
        MenuScreens.register(EnergyMenus.ENGINE_PEAT.menuType(), PeatEngineScreen::new);
        MenuScreens.register(EnergyMenus.ENGINE_SOLAR.menuType(), SolarEngineScreen::new);
        MenuScreens.register(FactoryMenuTypes.BOTTLER.menuType(), GuiBottler::new);
        MenuScreens.register(FactoryMenuTypes.CARPENTER.menuType(), GuiCarpenter::new);
        MenuScreens.register(FactoryMenuTypes.CENTRIFUGE.menuType(), GuiCentrifuge::new);
        MenuScreens.register(FactoryMenuTypes.FABRICATOR.menuType(), GuiFabricator::new);
        MenuScreens.register(FactoryMenuTypes.FERMENTER.menuType(), GuiFermenter::new);
        MenuScreens.register(FactoryMenuTypes.MOISTENER.menuType(), GuiMoistener::new);
        MenuScreens.register(FactoryMenuTypes.SMELTER.menuType(), GuiSmelter::new);
        MenuScreens.register(FactoryMenuTypes.SQUEEZER.menuType(), GuiSqueezer::new);
        MenuScreens.register(FactoryMenuTypes.STILL.menuType(), GuiStill::new);
        MenuScreens.register(SortingMenuTypes.GENETIC_FILTER.menuType(), GuiGeneticFilter::new);
        MenuScreens.register(WorktableMenus.WORKTABLE.menuType(), WorktableScreen::new);
        MenuScreens.register(ApicultureMenuTypes.ALVEARY.menuType(), AlvearyScreen::new);
        MenuScreens.register(ApicultureMenuTypes.ALVEARY_HYGROREGULATOR.menuType(), AlvearyHygroregulatorScreen::new);
        MenuScreens.register(ApicultureMenuTypes.ALVEARY_SIEVE.menuType(), AlvearySieveScreen::new);
        MenuScreens.register(ApicultureMenuTypes.ALVEARY_SWARMER.menuType(), AlvearySwarmerScreen::new);
        MenuScreens.register(ApicultureMenuTypes.BEE_HOUSING.menuType(), BeeHousingScreen::new);
    }
}
