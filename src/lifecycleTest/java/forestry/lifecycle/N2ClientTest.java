package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.core.genetics.IGenome;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;

import static forestry.lifecycle.PostalTests.check;

/** A separate client JVM; all data enters through production login and reload packets. */
@EventBusSubscriber(modid = LifecycleTests.ID, value = Dist.CLIENT)
public final class N2ClientTest {
	private static int ticks;
	private static int stepTicks;
	private static int step;
	private static String response = "";
	private static IGenome retained;
	private static Object trackerBeforeReconnect;
	private static boolean finished;
	private static java.util.List<net.minecraft.world.item.ItemStack> savedStacks;

	@SubscribeEvent
	public static void chat(ClientChatReceivedEvent event) {
		if (Boolean.getBoolean("forestry.lifecycle.n2Client") && event.getMessage().getString().startsWith("N2_")) {
			response = event.getMessage().getString();
			stepTicks = 0;
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event) throws Exception {
		if (!Boolean.getBoolean("forestry.lifecycle.n2Client") || finished) return;
		var client = Minecraft.getInstance();
		if (++ticks > 3600) throw new AssertionError("N2 client timeout: step=" + step + ", screen=" + client.gui.screen());
		if (client.level == null || client.player == null) return;
		if (++stepTicks < 40) return;
		if (step == 0) {
			check(client.getSingleplayerServer() == null, "Separate dedicated server required");
			N2RuntimeTest.checkData(false);
			retained = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST).getDefaultGenome();
			check(retained.isDefaultGenome(), "Prime client genome cache");
			command(client, "discover", 1);
		} else if (step == 1 && response.equals("N2_discover")) {
			checkTracker(client);
			savedStacks = N2WorldTest.specimenStacks();
			checkInventory(client);
			trackerBeforeReconnect = SpeciesUtil.BEE_TYPE.get().getBreedingTracker(client.level, client.player.getGameProfile());
			checkWorld(client);
			command(client, "machine", 9);
		} else if (step == 2 && response.equals("N2_change")) {
			N2RuntimeTest.checkData(true);
			check(!retained.isDefaultGenome(), "Remote reload invalidates retained genome cache");
			checkTracker(client);
			checkInventory(client);
			connect(client, 25573, 3);
		} else if (step == 3) {
			N2RuntimeTest.checkData(true);
			checkTracker(client);
			check(trackerBeforeReconnect != SpeciesUtil.BEE_TYPE.get().getBreedingTracker(client.level, client.player.getGameProfile()), "Logout clears client breeding tracker cache");
			checkInventory(client);
			connect(client, 25574, 6);
		} else if (step == 4 && response.equals("N2_restore")) {
			N2RuntimeTest.checkData(false);
			check(retained.isDefaultGenome(), "Remote pack removal restores default status");
			checkTracker(client);
			command(client, "done", 5);
		} else if (step == 5 && response.equals("N2_done")) {
			finished = true;
			Files.writeString(Path.of("n2-client.passed"), "passed");
			LogUtils.getLogger().info("FORESTRY_N2_CLIENT_READY login=true changed_reload=true removed_pack=true reconnect_same_process=true cross_server_A_B_A=true breeding_sync=true individual_components=3 datamaps=10");
			client.stop();
		} else if (step == 6) {
			N2RuntimeTest.checkData(false);
			var tracker = SpeciesUtil.BEE_TYPE.get().getBreedingTracker(client.level, client.player.getGameProfile());
			check(!tracker.isDiscovered(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST)) && !tracker.isDiscovered(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.COMMON)), "Server B has no discoveries from A");
			var peerItem = client.player.getInventory().getItem(0);
			check(peerItem.is(net.minecraft.world.item.Items.APPLE) && peerItem.getCount() == 9, "Server B has its own inventory");
			command(client, "done", 7);
		} else if (step == 7 && response.equals("N2_done")) {
			connect(client, 25573, 8);
		} else if (step == 8) {
			N2RuntimeTest.checkData(true);
			checkTracker(client);
			checkInventory(client);
			command(client, "restore", 4);
		} else if (step == 9 && response.equals("N2_machine")) {
			check(client.player.containerMenu instanceof forestry.core.content.machines.gui.ContainerStill, "Actual still menu opened");
			var still = (forestry.core.content.machines.tiles.TileStill) client.level.getBlockEntity(N2WorldTest.STILL);
			check(still.getEnergyManager().getEnergyStored() == 12345, "Machine GUI energy sync");
			check(still.getTankManager().getTank(1).getFluid().getAmount() == 2345, "Machine GUI tank sync");
			check(still.getItem(0).getCount() == 17 && still.getItem(0).getHoverName().getString().equals("N2 保存验证"), "Machine slot components sync");
			client.player.closeContainer();
			command(client, "change", 2);
		}
	}

	private static void checkWorld(Minecraft client) {
		var tree = (forestry.arboriculture.trees.TileTreeContainer) client.level.getBlockEntity(N2WorldTest.SAPLING);
		check(tree != null && tree.getTree() != null && tree.getTree().getSpecies().id().equals(forestry.api.arboriculture.ForestryTreeSpecies.BIRCH), "Sapling block entity species sync");
		for (var pos : java.util.List.of(N2WorldTest.FARM, N2WorldTest.ALVEARY)) {
			var member = (forestry.core.platform.multiblock.MultiblockTileEntityForestry<?>) client.level.getBlockEntity(pos);
			check(member != null && member.getController() != null && member.getController().isAssembled(), "Multiblock reconstructed from holder description: " + pos);
		}
		var bounds = new net.minecraft.world.phys.AABB(0, 78, 0, 16, 88, 16);
		var butterflies = client.level.getEntitiesOfClass(forestry.lepidopterology.entities.EntityButterfly.class, bounds, e -> e.getUUID().equals(N2WorldTest.BUTTERFLY));
		check(butterflies.size() == 1 && Math.abs(butterflies.getFirst().getSize() - 1.2f) < 0.001f, "Butterfly entity size sync");
		var boats = client.level.getEntitiesOfClass(forestry.arboriculture.entities.ForestryChestBoat.class, bounds, e -> e.getUUID().equals(N2WorldTest.BOAT));
		check(boats.size() == 1 && boats.getFirst().getWoodType() == forestry.arboriculture.wood.ForestryWoodType.BALSA, "Chest boat entity wood type sync");
	}

	private static void checkInventory(Minecraft client) {
		for (int i = 0; i < savedStacks.size(); i++) {
			var expected = savedStacks.get(i);
			var actual = client.player.getInventory().getItem(i);
			check(net.minecraft.world.item.ItemStack.matches(expected, actual), "Hybrid genome/mate/analyzed components synchronized for individual " + i
				+ " expected=" + forestry.core.platform.util.InventoryData.writeStack(expected, client.level.registryAccess())
				+ " actual=" + forestry.core.platform.util.InventoryData.writeStack(actual, client.level.registryAccess()));
		}
	}

	private static void connect(Minecraft client, int port, int next) {
		step = next;
		stepTicks = 0;
		response = "";
		String address = "127.0.0.1:" + port;
		ConnectScreen.startConnecting(new TitleScreen(), client, ServerAddress.parseString(address),
			new ServerData("N2 acceptance", address, ServerData.Type.OTHER), false, null);
	}

	private static void checkTracker(Minecraft client) {
		var tracker = SpeciesUtil.BEE_TYPE.get().getBreedingTracker(client.level, client.player.getGameProfile());
		check(tracker.isDiscovered(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST))
			&& tracker.isDiscovered(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.COMMON)), "Full/incremental breeding synchronization retains both discoveries");
	}

	private static void command(Minecraft client, String command, int next) {
		step = next;
		stepTicks = 0;
		response = "";
		client.getConnection().sendCommand("n2test " + command);
	}
}
