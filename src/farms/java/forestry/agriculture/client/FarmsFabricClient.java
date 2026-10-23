package forestry.agriculture.client;

import forestry.agriculture.features.MinifarmMenuTypes;
import forestry.agriculture.features.MultifarmMenuTypes;
import forestry.agriculture.minifarm.gui.MinifarmScreen;
import forestry.agriculture.multifarm.gui.MultifarmScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class FarmsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(MinifarmMenuTypes.PLANTER.menuType(), MinifarmScreen::new);
        MenuScreens.register(MultifarmMenuTypes.FARM.menuType(), MultifarmScreen::new);
    }
}
