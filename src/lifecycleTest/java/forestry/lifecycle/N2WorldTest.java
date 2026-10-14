package forestry.lifecycle;

import com.mojang.logging.LogUtils;
import forestry.agriculture.multifarm.multiblock.FarmController;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.apiculture.alveary.multiblock.AlvearyController;
import forestry.apiculture.bees.AbstractBeeHousingBlockEntity;
import forestry.arboriculture.entities.ForestryChestBoat;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.trees.TileTreeContainer;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.core.content.backpacks.features.BackpackItems;
import forestry.core.content.machines.tiles.TileStill;
import forestry.core.platform.multiblock.MultiblockTileEntityForestry;
import forestry.core.platform.multiblock.MultiblockValidation;
import forestry.core.platform.util.SpeciesUtil;
import forestry.lepidopterology.cocoons.TileCocoon;
import forestry.lepidopterology.entities.EntityButterfly;
import forestry.lepidopterology.features.LepidopterologyEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static forestry.lifecycle.PostalTests.check;

/** Real chunk/entity files, read by a second JVM. Fixtures live only in the opt-in N2 test world. */
@EventBusSubscriber(modid = LifecycleTests.ID)
public final class N2WorldTest {
	static final BlockPos CHEST = new BlockPos(2, 80, 2);
	static final BlockPos STILL = new BlockPos(4, 80, 2);
	static final BlockPos SAPLING = new BlockPos(6, 80, 2);
	static final BlockPos LEAVES = new BlockPos(8, 80, 2);
	static final BlockPos COCOON = new BlockPos(10, 80, 2);
	static final BlockPos APIARY = new BlockPos(12, 80, 2);
	static final BlockPos FARM = new BlockPos(3, 80, 8);
	static final BlockPos ALVEARY = new BlockPos(9, 80, 8);
	static final UUID BUTTERFLY = UUID.fromString("35518cb7-8276-41a2-9fca-e5ef0e3a3cba");
	static final UUID BOAT = UUID.fromString("75ef3cc4-7530-40d2-aeac-ec035a0eaa3b");
	private static final Path EXPECTED = Path.of("n2-world-expected.nbt");
	private static int ticks;
	private static boolean running;

	static void start(MinecraftServer server) {
		server.overworld().getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, server);
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
			server.overworld().setChunkForced(x, z, true);
			server.overworld().getChunk(x, z);
		}
		running = true;
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) throws Exception {
		if (!running || ++ticks < 80) return;
		running = false;
		var server = event.getServer();
		try {
			if (N2RuntimeTest.phase().equals("world-seed")) {
				seed(server.overworld());
				NbtIo.writeCompressed(snapshot(server.overworld()), EXPECTED);
				server.saveEverything(false, true, true);
			} else {
				check(Files.exists(EXPECTED), "Run world-seed before world-verify");
				CompoundTag expected = NbtIo.readCompressed(EXPECTED, NbtAccounter.unlimitedHeap());
				CompoundTag actual = snapshot(server.overworld());
				for (String key : expected.keySet()) check(expected.get(key).equals(actual.get(key)),
					"World restart changed " + key + ": expected=" + expected.get(key) + ", actual=" + actual.get(key));
			}
			N2RuntimeTest.passed();
			LogUtils.getLogger().info("FORESTRY_N2_WORLD_READY phase={} individuals=3 machine=true tree=true nursery=true cocoon=true apiary=true entities=2 multiblocks=2 backpack=true", N2RuntimeTest.phase());
		} finally {
			server.halt(false);
		}
	}

	static List<IIndividual> specimens() {
		var bee = SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.FOREST).createIndividual();
		bee = bee.copyWithGenome(bee.getGenome().copyWithPairs(Map.of(BeeChromosomes.FERTILITY,
			new AllelePair<>(new Allele<>(4, true), new Allele<>(2, false))))).cast();
		bee.setMate(SpeciesUtil.getBeeSpecies(ForestryBeeSpecies.MARSHY).getDefaultGenome());
		var tree = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.BIRCH).createIndividual();
		tree = tree.copyWithGenome(tree.getGenome().copyWith(Map.of(TreeChromosomes.HEIGHT, new Allele<>(1.7f, false)))).cast();
		tree.setMate(SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).getDefaultGenome());
		var butterfly = SpeciesUtil.BUTTERFLY_TYPE.get().getSpecies(Identifier.parse("forestry:brimstone")).createIndividual();
		butterfly = butterfly.copyWithGenome(butterfly.getGenome().copyWith(Map.of(ButterflyChromosomes.SIZE, new Allele<>(1.2f, true)))).cast();
		butterfly.setMate(SpeciesUtil.BUTTERFLY_TYPE.get().getDefaultSpecies().getDefaultGenome());
		List<IIndividual> result = List.of(bee, tree, butterfly);
		result.forEach(IIndividual::analyze);
		return result;
	}

	static List<ItemStack> specimenStacks() {
		var specimens = specimens();
		return List.of(specimens.get(0).createStack(BeeLifeStage.DRONE), specimens.get(1).createStack(TreeLifeStage.SAPLING),
			specimens.get(2).createStack(ButterflyLifeStage.BUTTERFLY));
	}

	private static void seed(ServerLevel level) {
		var specimens = specimens();
		var stacks = specimenStacks();
		level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
		var chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
		chest.clearContent();
		for (int i = 0; i < stacks.size(); i++) chest.setItem(i, stacks.get(i));
		ItemStack backpack = new ItemStack(BackpackItems.MINER_BACKPACK.item());
		backpack.set(DataComponents.CUSTOM_NAME, Component.literal("N2 矿工背包"));
		backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 17))));
		chest.setItem(3, backpack);
		var still = (TileStill) place(level, STILL, "still");
		still.setItem(0, namedDiamonds());
		still.getEnergyManager().setEnergyStored(12345);
		still.getTankManager().getTank(1).setFluid(new FluidStack(Fluids.WATER, 2345));
		((TileTreeContainer) place(level, SAPLING, "sapling_ge")).setTree((ITree) specimens.get(1));
		level.setBlockAndUpdate(SAPLING.below(), Blocks.DIRT.defaultBlockState());
		var leaves = (LeavesBlockEntity) place(level, LEAVES, "leaves");
		leaves.setTree((ITree) specimens.get(1));
		leaves.setCaterpillar((IButterfly) specimens.get(2));
		((TileCocoon) place(level, COCOON, "cocoon_solid")).setCaterpillar((IButterfly) specimens.get(2));
		var apiary = (AbstractBeeHousingBlockEntity) place(level, APIARY, "apiary");
		apiary.setItem(1, stacks.getFirst().copy());
		apiary.getOwnerHandler().setOwner(PostalTests.profile("N2Owner"));
		for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
			for (int y = 0; y < 4; y++) place(level, FARM.offset(x, y, z), x == 0 && y == 0 && z == 0 ? "stone_brick_farm_gearbox" : "stone_brick_farm_block");
			for (int y = 0; y < 3; y++) place(level, ALVEARY.offset(x, y, z), "alveary_block");
			level.setBlockAndUpdate(ALVEARY.offset(x, 3, z), Blocks.OAK_SLAB.defaultBlockState());
		}
		MultiblockValidation.validateAt(level, FARM);
		MultiblockValidation.validateAt(level, ALVEARY);
		var farm = (FarmController) member(level, FARM).getController();
		check(farm != null && farm.isAssembled(), "Real farm assembled");
		farm.getInternalInventory().setItem(12, namedDiamonds());
		farm.getTankManager().getTank(0).setFluid(new FluidStack(Fluids.WATER, 6789));
		var alveary = (AlvearyController) member(level, ALVEARY).getController();
		check(alveary != null && alveary.isAssembled(), "Real alveary assembled");
		alveary.getInternalInventory().setItem(1, stacks.getFirst().copy());
		for (var pos : List.of(CHEST, STILL, SAPLING, LEAVES, COCOON, APIARY, FARM, ALVEARY)) level.getBlockEntity(pos).setChanged();
		for (UUID id : List.of(BUTTERFLY, BOAT)) {
			var old = level.getEntity(id);
			if (old instanceof ForestryChestBoat chestBoat) chestBoat.clearContent();
			if (old != null) old.discard();
		}
		// Remove drops left by an interrupted fixture setup, before any test player can pick them up.
		discardFixtureDrops(level);
		var butterfly = EntityButterfly.create(LepidopterologyEntities.BUTTERFLY.entityType(), level, (IButterfly) specimens.get(2), LEAVES);
		butterfly.setUUID(BUTTERFLY);
		butterfly.setPos(10, 83, 3);
		butterfly.setNoAi(true);
		butterfly.setNoGravity(true);
		butterfly.setPersistenceRequired();
		butterfly.changeExhaustion(42);
		check(level.addFreshEntity(butterfly), "Butterfly spawned");
		var boat = new ForestryChestBoat(level, 3, 82, 4);
		boat.setUUID(BOAT);
		boat.setNoGravity(true);
		boat.setWoodType(ForestryWoodType.BALSA);
		boat.setItem(5, stacks.get(1).copy());
		check(level.addFreshEntity(boat), "Forestry chest boat spawned");
	}

	static BlockEntity place(ServerLevel level, BlockPos pos, String id) {
		var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("forestry:" + id));
		check(block != null && block != Blocks.AIR, "Fixture block registered: " + id);
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		return level.getBlockEntity(pos);
	}

	static void discardFixtureDrops(ServerLevel level) {
		for (var drop : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
			new net.minecraft.world.phys.AABB(0, 78, 0, 16, 88, 16))) drop.discard();
	}

	static MultiblockTileEntityForestry<?> member(ServerLevel level, BlockPos pos) {
		return (MultiblockTileEntityForestry<?>) level.getBlockEntity(pos);
	}

	private static ItemStack namedDiamonds() {
		var stack = new ItemStack(Items.DIAMOND, 17);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("N2 保存验证"));
		return stack;
	}

	private static CompoundTag snapshot(ServerLevel level) {
		CompoundTag result = new CompoundTag();
		for (var pos : List.of(CHEST, STILL, SAPLING, LEAVES, COCOON, APIARY)) {
			var tile = level.getBlockEntity(pos);
			check(tile != null, "Persisted tile exists: " + pos);
			result.put(pos.toShortString(), tile.saveCustomOnly(level.registryAccess()));
		}
		var farm = (FarmController) member(level, FARM).getController();
		var alveary = (AlvearyController) member(level, ALVEARY).getController();
		check(farm != null && farm.isAssembled() && farm.getComponents().size() == 36, "Farm rebuilt from chunk NBT");
		check(alveary != null && alveary.isAssembled() && alveary.getComponents().size() == 27, "Alveary rebuilt from chunk NBT");
		result.put("farmInventory", farm.getInternalInventory().write(new CompoundTag(), level.registryAccess()));
		result.put("farmTanks", farm.getTankManager().write(new CompoundTag(), level.registryAccess()));
		result.put("alvearyInventory", alveary.getInternalInventory().write(new CompoundTag(), level.registryAccess()));
		for (var controller : List.of(farm, alveary)) {
			long holders = controller.getComponents().stream().map(c -> (BlockEntity) c).filter(c -> c.saveCustomOnly(level.registryAccess()).contains(MultiblockTileEntityForestry.PAYLOAD_KEY)).count();
			check(holders == 1, "Exactly one member saves the shared payload");
		}
		var butterfly = (EntityButterfly) level.getEntity(BUTTERFLY);
		check(butterfly != null, "Butterfly loaded from entity region");
		result.put("butterfly", SpeciesUtil.serializeIndividual(butterfly.getButterfly()));
		result.putInt("exhaustion", butterfly.getExhaustion());
		result.putLong("home", butterfly.getHomePosition().asLong());
		var boat = (ForestryChestBoat) level.getEntity(BOAT);
		check(boat != null && boat.getWoodType() == ForestryWoodType.BALSA, "Chest boat wood type persisted");
		var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		boat.saveWithoutId(output);
		result.put("boatItems", output.buildResult().get("Items"));
		return result;
	}
}
