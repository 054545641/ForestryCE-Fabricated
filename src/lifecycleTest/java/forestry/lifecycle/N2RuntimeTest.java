package forestry.lifecycle;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import forestry.api.IForestryApi;
import forestry.api.ForestryDataMaps;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.ForestryFlowerTypes;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.SharedConstants;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static forestry.lifecycle.PostalTests.check;

/** Actual production definitions, reload listeners and SavedData in an isolated dedicated world. */
@EventBusSubscriber(modid = LifecycleTests.ID)
public final class N2RuntimeTest {
	private static final String PACK = "n2-acceptance";
	private static final Identifier EXTRA_FLOWER = Identifier.fromNamespaceAndPath(LifecycleTests.ID, "reload_flower");
	private static final Path EXPECTED = Path.of("n2-breeding-expected.nbt");
	private static boolean networkDone;
	private static int networkTicks;
	static String phase() { return System.getProperty("forestry.lifecycle.n2Phase", ""); }

	@SubscribeEvent
	public static void started(ServerStartedEvent event) throws Exception {
		if (phase().isEmpty()) return;
		var server = event.getServer();
		if (phase().startsWith("world-")) { N2WorldTest.start(server); return; }
		if (phase().equals("network") || phase().equals("peer")) {
			writePack(server);
			reload(server, false).whenCompleteAsync((unused, error) -> {
				if (error != null) { LogUtils.getLogger().error("FORESTRY_N2_FAILED", error); server.halt(false); return; }
				checkData(false);
				LogUtils.getLogger().info("FORESTRY_N2_NETWORK_LISTENING");
			}, server);
			return;
		}
		if (phase().equals("verify")) {
			check(Files.exists(EXPECTED), "Run n2Phase=seed first");
			check(NbtIo.readCompressed(EXPECTED, NbtAccounter.unlimitedHeap()).equals(snapshot(server)), "Three real breeding trackers retain exact records after process restart");
			checkData(false);
			LogUtils.getLogger().info("FORESTRY_N2_RESTART_READY breeding_types=3 player_and_common=true");
			passed();
			server.halt(false);
			return;
		}
		if (!phase().equals("seed")) throw new IllegalArgumentException("Unknown N2 phase: " + phase());
		// Remove only this fixture from the selected packs, including after an interrupted previous run.
		reload(server, false).thenComposeAsync(unused -> {
			checkData(false);
			IGenome retained = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST).getDefaultGenome();
			check(retained.isDefaultGenome(), "Prime default-genome cache before reload");
			seedTrackers(server);
			CompoundTag before = snapshot(server);
			try { writePack(server); } catch (Exception e) { throw new RuntimeException(e); }
			return reload(server, true).thenComposeAsync(ignored -> {
				checkData(true);
				check(retained.getActiveValue(BeeChromosomes.FERTILITY) == 3, "Existing individual's inline allele remains unchanged");
				check(retained.getActiveSpecies() == SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST), "Retained genome resolves the new species definition");
				check(!retained.isDefaultGenome(), "Default-genome cache must follow changed species definitions");
				check(before.equals(snapshot(server)), "Reload preserves discoveries and research");
				return reload(server, false).thenRunAsync(() -> {
					checkData(false);
					check(retained.isDefaultGenome(), "Default-genome status recovers after pack removal");
					check(before.equals(snapshot(server)), "Pack removal preserves breeding records");
					try { NbtIo.writeCompressed(before, EXPECTED); } catch (Exception e) { throw new RuntimeException(e); }
					server.saveEverything(false, true, true);
					LogUtils.getLogger().info("FORESTRY_N2_SAVE_READY changed_reload=true removed_pack=true genome_cache=true breeding_types=3 player_and_common=true");
					passed();
				}, server);
			}, server);
		}, server).whenCompleteAsync((unused, error) -> {
			if (error != null) LogUtils.getLogger().error("FORESTRY_N2_FAILED", error);
			server.halt(false);
		}, server);
	}

	@SubscribeEvent
	public static void timeout(ServerTickEvent.Post event) {
		if ((phase().equals("network") || phase().equals("peer")) && ++networkTicks > 6000) {
			LogUtils.getLogger().error("FORESTRY_N2_FAILED network timeout");
			event.getServer().halt(false);
		}
	}

	@SubscribeEvent
	public static void login(PlayerEvent.PlayerLoggedInEvent event) {
		if (phase().equals("peer") && event.getEntity() instanceof ServerPlayer player && player.getGameProfile().name().equals("N2Client")) {
			player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(Items.APPLE, 9));
		}
	}

	@SubscribeEvent
	public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
		if ((phase().equals("network") || phase().equals("peer")) && networkDone && event.getEntity() instanceof ServerPlayer player
			&& player.getGameProfile().name().equals("N2Client")) player.level().getServer().halt(false);
	}

	@SubscribeEvent
	public static void commands(RegisterCommandsEvent event) {
		if (!phase().equals("network") && !phase().equals("peer")) return;
		event.getDispatcher().register(Commands.literal("n2test")
			.requires(source -> source.getEntity() instanceof ServerPlayer player && player.getGameProfile().name().equals("N2Client"))
			.then(Commands.argument("step", StringArgumentType.word()).executes(context -> {
				var player = context.getSource().getPlayerOrException();
				var server = player.level().getServer();
				String step = StringArgumentType.getString(context, "step");
				if (step.equals("change") || step.equals("restore")) {
					boolean changed = step.equals("change");
					reload(server, changed).whenCompleteAsync((unused, error) -> {
						if (error != null) { LogUtils.getLogger().error("FORESTRY_N2_FAILED", error); server.halt(false); return; }
						checkData(changed);
						player.sendSystemMessage(Component.literal("N2_" + step));
					}, server);
				} else if (step.equals("discover")) {
					N2WorldTest.discardFixtureDrops(player.level());
					for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
						var floor = new net.minecraft.core.BlockPos(x, 79, z);
						if (player.level().isEmptyBlock(floor)) player.level().setBlockAndUpdate(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
					}
					player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
					player.teleportTo(player.level(), 5.5, 80, 4.5, java.util.Set.of(), 180, 15, true);
					var stacks = N2WorldTest.specimenStacks();
					for (int i = 0; i < stacks.size(); i++) player.getInventory().setItem(i, stacks.get(i));
					var tracker = SpeciesUtil.BEE_TYPE.get().getBreedingTracker(player.level(), player.getGameProfile());
					tracker.registerSpecies(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST));
					tracker.syncToPlayer(player);
					tracker.registerSpecies(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.COMMON));
					player.sendSystemMessage(Component.literal("N2_discover"));
				} else if (step.equals("machine")) {
					var tile = (forestry.core.content.machines.tiles.TileStill) player.level().getBlockEntity(N2WorldTest.STILL);
					check(tile != null, "Run N2 world-seed before network acceptance");
					player.openMenu(tile, N2WorldTest.STILL);
					player.sendSystemMessage(Component.literal("N2_machine"));
				} else if (step.equals("done")) {
					server.saveEverything(false, true, true);
					networkDone = true;
					passed();
					LogUtils.getLogger().info("FORESTRY_N2_NETWORK_READY");
					player.sendSystemMessage(Component.literal("N2_done"));
				} else throw new IllegalArgumentException(step);
				return 1;
			})));
	}

	static void passed() {
		try { Files.writeString(Path.of("n2-" + phase() + ".passed"), "passed"); }
		catch (Exception e) { throw new RuntimeException(e); }
	}

	static CompletableFuture<Void> reload(MinecraftServer server, boolean enabled) {
		server.getPackRepository().reload();
		var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
		selected.remove("file/" + PACK);
		if (enabled) selected.add("file/" + PACK);
		return server.reloadResources(selected);
	}

	static void writePack(MinecraftServer server) throws Exception {
		Path root = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK);
		var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
		var metadata = new JsonObject();
		metadata.add("pack", PackMetadataSection.SERVER_TYPE.codec().encodeStart(JsonOps.INSTANCE,
			new PackMetadataSection(Component.literal("N2 isolated reload fixture"), new InclusiveRange<>(format))).getOrThrow());
		write(root, "pack.mcmeta", metadata.toString());
		write(root, "data/forestry/bee_species/forest.json", """
			{"dominant":true,"genome":{"forestry:fertility":{"value":7}},"genus":"apis","outline":1691884,"species":"nigrocincta"}
			""");
		write(root, "data/forestry/recipe/bee_mutation/common_1.json", """
			{"type":"forestry:bee_mutation","chance":0.42,"first":"forestry:forest","id":"forestry:bee_mutation/common_1","result":"forestry:common","second":"forestry:marshy"}
			""");
		String flower = "{\"type\":\"forestry:tag_flower_type\",\"dominant\":false,\"flowers\":\"forestry:flowers/vanilla\"}";
		write(root, "data/forestry/flower_type/flower_type_vanilla.json", flower);
		write(root, "data/" + LifecycleTests.ID + "/flower_type/reload_flower.json", flower);
		write(root, "data/forestry/data_maps/item/postage.json", "{\"values\":{\"minecraft:apple\":13}}");
		N2ReloadData.write(root);
	}

	private static void write(Path root, String relative, String contents) throws Exception {
		Path file = root.resolve(relative);
		Files.createDirectories(file.getParent());
		Files.writeString(file, contents);
	}

	static void checkData(boolean changed) {
		N2ReloadData.checkData(changed);
		var forest = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST);
		check(forest.getDefaultGenome().getActiveValue(BeeChromosomes.FERTILITY) == (changed ? 7 : 3), "Species default allele follows pack state");
		var mutations = SpeciesUtil.BEE_TYPE.get().getMutations();
		var marshy = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.MARSHY);
		var common = mutations.getCombinations(forest, marshy).stream().filter(m -> m.getResult().id().equals(ForestryBeeSpecies.COMMON)).findFirst().orElseThrow();
		check(Math.abs(common.getChance() - (changed ? 0.42f : 0.15f)) < 0.0001f, "Mutation recipe chance and index follow current species identities");
		check(common.getFirstParent() == forest || common.getSecondParent() == forest, "Mutation references live parent");
		var flowers = IForestryApi.INSTANCE.getFlowerTypeManager();
		check(flowers.getFlowerType(ForestryFlowerTypes.VANILLA).isDominant() != changed, "Flower override is restored after removal");
		check((flowers.getFlowerType(EXTRA_FLOWER) != null) == changed, "Removed datapack-only flower does not remain cached");
		Integer postage = Items.APPLE.builtInRegistryHolder().getData(ForestryDataMaps.POSTAGE);
		check(changed ? Integer.valueOf(13).equals(postage) : postage == null, "DataMap addition/removal follows pack state");
	}

	private static void seedTrackers(MinecraftServer server) {
		for (var type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			for (boolean common : new boolean[]{false, true}) {
				var tracker = type.getBreedingTracker(server.overworld(), common ? null : PostalTests.profile("N2Breeder"));
				CompoundTag before = new CompoundTag();
				tracker.save(before);
				tracker.registerBirth(type.getDefaultSpecies());
				var mutation = type.getMutations().getAllMutations().getFirst();
				tracker.registerSpecies(mutation.getResult());
				tracker.researchMutation(mutation);
				check(tracker.isDiscovered(mutation) && tracker.isResearched(mutation), "Mutation discovery and research registered: " + type.id());
				CompoundTag after = new CompoundTag();
				tracker.save(after);
				check(before.equals(after) || ((SavedData) tracker).isDirty(), "Changed tracker marked dirty");
			}
		}
	}

	private static CompoundTag snapshot(MinecraftServer server) {
		CompoundTag result = new CompoundTag();
		var types = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes();
		check(types.size() == 3, "All three breeding tracker implementations loaded");
		for (var type : types) {
			for (boolean common : new boolean[]{false, true}) {
				CompoundTag data = new CompoundTag();
				type.getBreedingTracker(server.overworld(), common ? null : PostalTests.profile("N2Breeder")).save(data);
				result.put(type.id() + (common ? "/common" : "/player"), data);
			}
		}
		return result;
	}
}
