package forestry.core.platform.config;

import org.apache.commons.lang3.tuple.Pair;

public class ForestryConfig {
	private static final ConfigSpec CLIENT_SPEC;
	private static final ConfigSpec SERVER_SPEC;
    public static void validateServerSnapshot(com.google.gson.JsonObject values) { SERVER_SPEC.validate(values, true); }
	public static final Client CLIENT;
	public static final Server SERVER;

	public static class Client {
		// Misc
		public final ConfigSpec.BooleanValue showParticles;
		public final ConfigSpec.BooleanValue enableHints;
		public final ConfigSpec.BooleanValue enableGlints;
		// Mail
		public final ConfigSpec.BooleanValue mailAlertsEnabled;
		// JEI Bees
		public final ConfigSpec.BooleanValue showRequirements;
		public final ConfigSpec.BooleanValue showSecretMutations;
		public final ConfigSpec.BooleanValue identifyGenome;

		public Client(ConfigSpec.Builder builder) {
			this.showParticles = builder
				.comment("Whether any of Forestry's particles are rendered.")
				.define("particles", true);
			this.enableHints = builder
				.comment("Whether the \"Did you know?\" ledgers are shown in Forestry menus.")
				.define("enable_hints", true);
			this.enableGlints = builder
				.comment("Whether certain specimens, like Imperial Bees, will have enchantment glints on them.")
				.define("enable_glints", true);

			this.mailAlertsEnabled = builder
				.comment("Whether alerts are enabled for Forestry's mail system.")
				.define("mail_alerts_enable", true);

			builder.push("jei_bees");
			this.showRequirements = builder
				.comment("Set to false to disable display of mutation requirements")
				.define("show_requirements", true);
			this.showSecretMutations = builder
				.comment("Set to false to disable display of secret mutations")
				.define("show_secret_mutations", true);
			this.identifyGenome = builder
				.comment("Set to false to disable showing identified genome in recipes")
				.define("show_analyzed_tooltip", true);
			builder.pop();
		}
	}

	public static class Server {
		// Genetics
		public final ConfigSpec.DoubleValue researchMutationBoostMultiplier;
		public final ConfigSpec.DoubleValue maxResearchMutationBoostPercent;
		// Bees
		public final ConfigSpec.BooleanValue pollinateVanillaLeaves;
		public final ConfigSpec.DoubleValue wildHiveSpawnRate;
		public final ConfigSpec.BooleanValue useHaploidDrones;
		public final ConfigSpec.DoubleValue hiveSecondPrincessChance;
		// Trees
		public final ConfigSpec.DoubleValue treesSpawnNaturally;
		// Butterflies
		public final ConfigSpec.BooleanValue disableButterflySpawning;
		public final ConfigSpec.IntValue butterflyClusterLimit;
		public final ConfigSpec.IntValue butterflyClusterWidth;
		public final ConfigSpec.IntValue butterflyClusterHeight;
		// Farms
		public final ConfigSpec.IntValue multiFarmSize;
		public final ConfigSpec.BooleanValue squareMultiFarms;
		public final ConfigSpec.IntValue legacyFarmsPlanterRings;
		public final ConfigSpec.BooleanValue legacyFarmsUseRings;
		public final ConfigSpec.IntValue legacyFarmsRingSize;
		// Misc
		public final ConfigSpec.BooleanValue enableBackpackResupply;
		public final ConfigSpec.BooleanValue spawnTinOre;
		public final ConfigSpec.BooleanValue spawnApatiteOre;
		public final ConfigSpec.DoubleValue escritoireBountyMultiplier;
		// Solar Engine
		public final ConfigSpec.DoubleValue solarFE;
		public final ConfigSpec.DoubleValue twilightSolarFE;
		public final ConfigSpec.DoubleValue solarArrayBonusFactor;

		public Server(ConfigSpec.Builder builder) {
			// Genetics
			builder.push("genetics");
			this.researchMutationBoostMultiplier = builder
				.comment("When a player researches a mutation using the Escritoire, mutation chances for hives owned by that player are multiplied by this factor, with the increase in chance limited to the value set in \"research_mutation_boost_multiplier\".")
				.defineInRange("research_mutation_boost_multiplier", 1.5, 1.0, 1000.0);
			this.maxResearchMutationBoostPercent = builder
				.comment("When a player researchs a mutation using the Escritoire, mutation chances for hives owned by that player are multiplied by a certain factor, with the increase in chance capped to this value.")
				.defineInRange("max_research_mutation_boost_percent", 5.0, 0.0, 100.0);
			builder.pop();

			// Bees
			builder.push("bees");
			this.pollinateVanillaLeaves = builder
				.comment("Whether bees and butterflies can pollinate Vanilla leaves. Might be undesirable for builds that rely on leaves.")
				.define("pollinate_vanilla_leaves", true);
			this.wildHiveSpawnRate = builder
				.comment("The base chance for a wild beehive to spawn naturally.")
				.defineInRange("wild_hive_spawn_rate", 1.0, 0.0, 1000.0);
			this.useHaploidDrones = builder
				.comment("In real life, drone bees are haploid, which means they only carry one set of chromosomes. If this option is enabled, only a drone's active alleles will be used for inheritance, making drones effectively haploid. This CHANGES Forestry's bee breeding mechanics.")
				.define("use_haploid_drones", true);
			this.hiveSecondPrincessChance = builder
				.comment("The probability between 0 and 1 that an additional princess drops from a wild hive. 0 by default, meaning no second princess, ever.")
				.defineInRange("hive_second_princess_chance", 0.0, 0.0, 1.0);
			builder.pop();

			// Farming
			builder.push("farming");
			this.multiFarmSize = builder
				.comment("")
				.defineInRange("multiblock_farm_size", 2, 1, 10);
			this.squareMultiFarms = builder
				.comment("Whether Forestry multiblock farms have square shaped farmlands instead of the default diamond shape.")
				.define("square_multiblock_farms", false);
			this.legacyFarmsPlanterRings = builder
				.comment("Sets the size of the farmland that is used by all legacy (single block) farms.")
				.defineInRange("legacy_farms_planter_rings", 4, 1, 10);
			this.legacyFarmsUseRings = builder
				.comment("Whether legacy (single block) farms use a ring layout. The farmland size of the ring layout is always one block smaller.")
				.define("legacy_farms_use_rings", true);
			this.legacyFarmsRingSize = builder
				.comment("Sets the size of the inner ring of the ring layout.")
				.defineInRange("legacy_farms_ring_size", 4, 1, 10);
			builder.pop();

			// Trees
			builder.push("trees");
			this.treesSpawnNaturally = builder
				.comment("Multiplies the chance of a Forestry tree spawning in the wild. Set to 0 to disable Forestry tree spawning.")
				.defineInRange("tree_spawn_chance_modifier", 0.0f, 0.0f, 1000000.0f);
			builder.pop();

			// Butterflies
			builder.push("butterflies");
			this.disableButterflySpawning = builder
				.comment("Whether butterflies can spawn from Forestry leaves.")
				.define("disable_butterfly_spawning", false);
			this.butterflyClusterLimit = builder
				.comment("The maximum number of butterflies that can spawn in the same area or cluster.")
				.defineInRange("butterfly_cluster_limit", 20, 1, 2000);
			this.butterflyClusterWidth = builder
				.comment("The width of the cluster area used when checking if the \"butterfly_cluster_limit\" has been reached.")
				.defineInRange("butterfly_cluster_width", 128, 0, 2000);
			this.butterflyClusterHeight = builder
				.comment("The height of the cluster area used when checking if the \"butterfly_cluster_limit\" has been reached.")
				.defineInRange("butterfly_cluster_height", 64, 0, 2000);
			builder.pop();

			// Misc
			this.enableBackpackResupply = builder
				.comment("Whether backpacks can have their resupply mode enabled, which stocks a player's inventory using blocks from the backpack's inventory.")
				.define("enable_backpack_resupply", true);
			this.spawnTinOre = builder
				.comment("Whether Tin Ore veins generate naturally in the Overworld.")
				.define("spawn_tin_ore", true);
			this.spawnApatiteOre = builder
				.comment("Whether Apatite Ore veins generate naturally in the Overworld.")
				.define("spawn_apatite_ore", true);
			this.escritoireBountyMultiplier = builder
				.comment("Multiplies the chance of a reward from winning escritoire game(does not affect mutation notes)")
				.defineInRange("escritoire_bounty_multiplier", 1f, 0.0f, 1000f);

			// Solar Engine
			builder.push("solar_engine");
			this.solarFE = builder
				.comment("How much FE/t a single solar panel generates.")
				.defineInRange("solar_fe_per_panel", 6.0, 0.001, 10000000.0);
			this.twilightSolarFE = builder
				.comment("How much FE/t a single solar panel generates in the Twilight Forest.")
				.defineInRange("twilight_solar_fe_per_panel", 3, 0.001, 10000000.0);
			this.solarArrayBonusFactor = builder
				.comment("A solar array generates a bonus on top of the flat output of its panels. The bonus is this factor multiplied by the square of one less than the number of lit panels, so at the default value an array of 100 lit panels adds 0.03 * 99^2 = 294.03 FE/t. Insolation scales the bonus the same way it scales the flat output. Set to 0 to disable.")
				.defineInRange("solar_array_bonus_factor", 0.03, 0.0, 10000000.0);
			builder.pop();
		}
	}

    public static void initialize() {
        var directory = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("forestry");
        try {
            SERVER_SPEC.load(directory.resolve("server.json"), directory.getParent().resolve("forestry-server.toml"));
            CLIENT_SPEC.load(directory.resolve("client.json"), directory.getParent().resolve("forestry-client.toml"));
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot load Forestry configuration", error);
        }
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            try {
                // Reset to the installation template for every world before reading its own overrides.
                SERVER_SPEC.load(directory.resolve("server.json"));
                var world = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("serverconfig");
                SERVER_SPEC.load(world.resolve("forestry-server.json"), world.resolve("forestry-server.toml"));
            } catch (java.io.IOException error) {
                throw new IllegalStateException("Cannot load world Forestry configuration", error);
            }
        });
    }
    public static com.google.gson.JsonObject serverSnapshot() { return SERVER_SPEC.snapshot(); }
    public static void applyServerSnapshot(com.google.gson.JsonObject values) { SERVER_SPEC.applyRemote(values); }
    public static void clearServerSnapshot() { SERVER_SPEC.clearRemote(); }

	static {
		{
			Pair<Client, ConfigSpec> specPair = new ConfigSpec.Builder().configure(Client::new);
			CLIENT = specPair.getLeft();
			CLIENT_SPEC = specPair.getRight();
		}
		{
			Pair<Server, ConfigSpec> specPair = new ConfigSpec.Builder().configure(Server::new);
			SERVER = specPair.getLeft();
			SERVER_SPEC = specPair.getRight();
		}
	}
}
