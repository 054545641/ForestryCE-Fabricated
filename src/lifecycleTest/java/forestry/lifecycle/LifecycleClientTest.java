package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.api.IForestryApi;
import forestry.core.platform.tab.ForestryCreativeTabs;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;

import java.util.List;

@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class LifecycleClientTest {
	private static int ticks;
	private static int worldTicks;
	private static boolean finished;
	private static final List<String> CREATIVE_TABS = List.of("forestry", "building_blocks", "arboriculture", "lepidopterology", "agriculture", "storage", "mail", "apiculture");

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) {
		if (Boolean.getBoolean("forestry.lifecycle.escritoireTest")) return;
		if (Boolean.getBoolean("forestry.lifecycle.powerTest")) return;
		if (Boolean.getBoolean("forestry.lifecycle.analyzerTest")) return;
		if (!Boolean.getBoolean("forestry.lifecycle.clientAutoExit") || finished) return;
		Minecraft client = Minecraft.getInstance();
		if (Boolean.getBoolean("forestry.lifecycle.creativeTest")) {
			testCreativeInventory(client);
			return;
		}
		if (client.level != null && client.player != null && ++worldTicks == 60) {
			finished = true;
			if (SpeciesUtil.TREE_TYPE.get().getAllSpecies().size() != 50
				|| IForestryApi.INSTANCE.getCircuitManager().getCircuitHolders().size() != 25) {
				throw new AssertionError("Client world has incomplete Forestry data");
			}
			LogUtils.getLogger().info("FORESTRY_CLIENT_WORLD_READY dimension={} player={}", client.level.dimension().identifier(), client.player.getName().getString());
			Screenshot.grab(client.gameDirectory, "lifecycle-world.png", client.gameRenderer.mainRenderTarget(), 1,
				message -> client.execute(client::stop));
		} else if (++ticks > 2400) {
			finished = true;
			LogUtils.getLogger().error("FORESTRY_CLIENT_WORLD_FAILED screen={}", client.gui.screen());
			client.stop();
		}
	}

	private static void testCreativeInventory(Minecraft client) {
		if (++ticks > 2400) throw new AssertionError("Creative inventory test timed out: " + client.gui.screen());
		if (client.level == null || client.player == null) return;
		worldTicks++;
		if (worldTicks == 40) {
			var server = client.getSingleplayerServer();
			if (server == null) throw new AssertionError("Creative test needs the isolated integrated world");
			var playerId = client.player.getUUID();
			server.execute(() -> server.getPlayerList().getPlayer(playerId).setGameMode(GameType.CREATIVE));
		}
		if (worldTicks == 80 || worldTicks == 120) {
			if (!client.player.hasInfiniteMaterials()) throw new AssertionError("Creative mode did not synchronize");
			// Use the same key binding and InventoryScreen redirect as pressing E in game.
			KeyMapping.click(client.options.keyInventory.getKey());
		} else if (worldTicks == 100 || worldTicks == 140) {
			if (!(client.gui.screen() instanceof CreativeModeInventoryScreen)) {
				throw new AssertionError("E did not open the creative inventory: " + client.gui.screen());
			}
			CreativeTabChecks.checkContents();
			if (worldTicks == 100) {
				client.player.closeContainer();
			}
		}
		if (worldTicks >= 140 && worldTicks < 300 && worldTicks % 20 == 0) {
			selectCreativeTab((CreativeModeInventoryScreen) client.gui.screen(), CREATIVE_TABS.get((worldTicks - 140) / 20));
		} else if (worldTicks == 300) {
			finished = true;
			LogUtils.getLogger().info("FORESTRY_CREATIVE_INVENTORY_READY opened_twice=true rendered_tabs=8");
			Screenshot.grab(client.gameDirectory, "lifecycle-creative.png", client.gameRenderer.mainRenderTarget(), 1,
				message -> client.execute(client::stop));
		}
	}

	private static void selectCreativeTab(CreativeModeInventoryScreen screen, String name) {
		var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(ForestryCreativeTabs.tabKey(name));
		// Reproduce NeoForge's ten-tab page grouping, then exercise the screen's real tab click handler.
		var tabs = CreativeModeTabRegistry.getSortedCreativeModeTabs().stream().filter(CreativeModeTab::hasAnyItems).toList();
		int start = (tabs.indexOf(tab) / 10) * 10;
		var page = new CreativeTabsScreenPage(tabs.subList(start, Math.min(start + 10, tabs.size())));
		screen.setCurrentPage(page);
		double x = screen.getGuiLeft() + 27 * page.getColumn(tab) + 13;
		double y = screen.getGuiTop() + (page.isTop(tab) ? -32 : screen.getYSize()) + 16;
		if (!screen.mouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)))
			|| screen.getMenu().items.size() != tab.getDisplayItems().size()) {
			throw new AssertionError("Could not select creative tab: " + name);
		}
		LogUtils.getLogger().info("FORESTRY_CREATIVE_TAB_SELECTED tab={} items={}", name, screen.getMenu().items.size());
	}
}
