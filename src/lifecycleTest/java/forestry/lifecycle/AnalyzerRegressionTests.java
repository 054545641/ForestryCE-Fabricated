package forestry.lifecycle;

import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.ForestryTreeSpecies;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.arboriculture.sapling.SaplingBlockEntity;
import forestry.core.content.analyzer.ContainerAnalyzer;
import forestry.core.content.analyzer.TileAnalyzer;
import forestry.core.platform.inventory.InventoryAnalyzer;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Player-facing inputs, repeated specimens and planting with the production registries. */
final class AnalyzerRegressionTests {

	static void vanillaSaplings(GameTestHelper h) {
		TileAnalyzer analyzer = analyzer(h);
		Player player = FakePlayerFactory.getMinecraft(h.getLevel());
		var menu = new ContainerAnalyzer(1, player.getInventory(), analyzer);
		for (int batch = 0; batch < 2; batch++) {
			ItemStack saplings = new ItemStack(Items.OAK_SAPLING, 3);
			h.assertTrue(menu.getForestrySlot(0).mayPlace(saplings), "Fresh vanilla oak saplings accepted, batch " + batch);
			player.getInventory().setItem(9, saplings);
			menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
			h.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift-click inserts vanilla saplings");
			completeAnalysis(h, analyzer, 3 * batch);
			h.assertTrue(analyzer.getItem(InventoryAnalyzer.SLOT_OUTPUT_1).getCount() == 3 * (batch + 1), "Both batches produce analyzed saplings");
		}
		// Automation must accept the same raw sapling as the menu, including after prior analysis.
		var handler = analyzer.getItemHandler(null);
		try (var tx = Transaction.openRoot()) {
			h.assertTrue(handler.insert(InventoryAnalyzer.SLOT_INPUT_1, ItemResource.of(Items.OAK_SAPLING), 1, tx) == 1,
				"Automation accepts a new vanilla sapling");
			tx.commit();
		}
		completeAnalysis(h, analyzer, 6);
		h.succeed();
	}

	static void repeatedBees(GameTestHelper h) {
		TileAnalyzer analyzer = analyzer(h);
		Player player = FakePlayerFactory.getMinecraft(h.getLevel());
		var menu = new ContainerAnalyzer(2, player.getInventory(), analyzer);
		var drop = forestry.api.IForestryApi.INSTANCE.getHiveManager().getDrops(ForestryBeeSpecies.FOREST).getFirst();
		for (int batch = 0; batch < 2; batch++) {
			var bee = drop.createIndividual(h.getLevel(), analyzer.getBlockPos());
			bee.setPristine(true);
			ItemStack princess = bee.createStack(BeeLifeStage.PRINCESS);
			h.assertTrue(!IIndividualHandlerItem.getIndividual(princess).isAnalyzed(), "A new hive drop is not analyzed");
			player.getInventory().setItem(9, princess);
			menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
			h.assertTrue(player.getInventory().getItem(9).isEmpty(), "Shift-click inserts new princess");
			completeAnalysis(h, analyzer, batch);
		}
		// Only an already analyzed individual may pass straight through without honey or energy.
		ItemStack known = analyzer.removeItem(InventoryAnalyzer.SLOT_OUTPUT_1, 1);
		player.getInventory().setItem(9, known);
		menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		int honey = analyzer.getTankManager().getTank(0).getFluidAmount();
		int energy = analyzer.getEnergyManager().getEnergyStored();
		h.assertTrue(analyzer.processWorkStep() && analyzer.getItem(InventoryAnalyzer.SLOT_ANALYZE).isEmpty(), "Known individual passes through");
		h.assertTrue(analyzer.getTankManager().getTank(0).getFluidAmount() == honey
			&& analyzer.getEnergyManager().getEnergyStored() == energy, "Known individual costs no honey or energy");
		var freshBee = drop.createIndividual(h.getLevel(), analyzer.getBlockPos());
		freshBee.setPristine(true);
		player.getInventory().setItem(9, freshBee.createStack(BeeLifeStage.PRINCESS));
		menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);
		completeAnalysis(h, analyzer, 2);
		h.succeed();
	}

	static void plantAnalyzedOak(GameTestHelper h) {
		var level = h.getLevel();
		Player player = FakePlayerFactory.getMinecraft(level);
		BlockPos soil = h.absolutePos(new BlockPos(2, 1, 2));
		for (var ground : new net.minecraft.world.level.block.Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.MOSS_BLOCK}) {
			level.setBlockAndUpdate(soil, ground.defaultBlockState());
			for (int height = 1; height <= 6; height++) level.setBlockAndUpdate(soil.above(height), Blocks.AIR.defaultBlockState());
			var tree = SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual();
			tree.analyze();
			player.setItemInHand(InteractionHand.MAIN_HAND, tree.createStack(TreeLifeStage.SAPLING));
			player.setPos(soil.getX() + 0.5, soil.getY() + 2, soil.getZ() + 0.5);
			player.setXRot(90);
			h.assertTrue(tree.canStay(level, soil.above()), "Oak supports vanilla planting soil: " + ground);
			h.assertTrue(IIndividualHandlerItem.getIndividual(player.getMainHandItem()).isAnalyzed(), "Planted item is analyzed");
			player.getMainHandItem().getItem().use(level, player, InteractionHand.MAIN_HAND);
			h.assertTrue(level.getBlockEntity(soil.above()) instanceof SaplingBlockEntity sapling
				&& sapling.getTree() != null && sapling.getTree().getGenome().equals(tree.getGenome()), "Actual item use plants analyzed oak with its genome: " + ground);
		}
		level.setBlockAndUpdate(soil, Blocks.STONE.defaultBlockState());
		h.assertTrue(!SpeciesUtil.getTreeSpecies(ForestryTreeSpecies.OAK).createIndividual().canStay(level, soil.above()), "Stone is not planting soil");
		h.succeed();
	}

	private static TileAnalyzer analyzer(GameTestHelper h) {
		var analyzer = (TileAnalyzer) N2WorldTest.place(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1)), "analyzer");
		analyzer.getTankManager().getTank(0).setFluid(new FluidStack(BuiltInRegistries.FLUID.getValue(Identifier.parse("forestry:honey")), 1000));
		analyzer.getEnergyManager().setEnergyStored(analyzer.getEnergyManager().getMaxEnergyStored());
		return analyzer;
	}

	private static void completeAnalysis(GameTestHelper h, TileAnalyzer analyzer, int previousOutput) {
		analyzer.getEnergyManager().setEnergyStored(analyzer.getEnergyManager().getMaxEnergyStored());
		int honey = analyzer.getTankManager().getTank(0).getFluidAmount();
		int energy = analyzer.getEnergyManager().getEnergyStored();
		h.assertTrue(analyzer.processWorkStep(), "New specimen starts work");
		h.assertTrue(analyzer.getStepsPerWorkCycle() == 125 && analyzer.getWorkCounter() == 1, "Each new specimen requires a complete cycle");
		int inputCount = analyzer.getItem(InventoryAnalyzer.SLOT_ANALYZE).getCount();
		for (int step = 1; step < 124; step++) h.assertTrue(analyzer.processWorkStep(), "Analysis progresses");
		h.assertTrue(outputCount(analyzer) == previousOutput, "No early output from a repeated species");
		h.assertTrue(!IIndividualHandlerItem.getIndividual(analyzer.getItem(InventoryAnalyzer.SLOT_ANALYZE)).isAnalyzed(), "Working specimen stays unknown until completion");
		h.assertTrue(analyzer.processWorkStep() && analyzer.getWorkCounter() == 0, "Analysis finishes on final step");
		h.assertTrue(outputCount(analyzer) == previousOutput + inputCount, "Exactly the input batch reaches output");
		h.assertTrue(analyzer.getTankManager().getTank(0).getFluidAmount() == honey - 100, "Each batch consumes 100 mB honey");
		h.assertTrue(analyzer.getEnergyManager().getEnergyStored() < energy, "Each new specimen consumes energy");
		h.assertTrue(IIndividualHandlerItem.getIndividual(analyzer.getItem(InventoryAnalyzer.SLOT_OUTPUT_1)).isAnalyzed(), "Output is analyzed");
	}

	private static int outputCount(TileAnalyzer analyzer) {
		int count = 0;
		for (int slot = InventoryAnalyzer.SLOT_OUTPUT_1; slot < InventoryAnalyzer.SLOT_OUTPUT_1 + InventoryAnalyzer.SLOT_OUTPUT_COUNT; slot++) count += analyzer.getItem(slot).getCount();
		return count;
	}
}
