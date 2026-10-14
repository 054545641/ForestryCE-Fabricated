package forestry.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import forestry.api.ForestryConstants;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.blocks.SolarPanelBlock;
import forestry.core.content.energy.features.EnergyBlocks;
import forestry.core.content.energy.tiles.SolarEngineBlockEntity;

/**
 * Covers the solar engine claiming and releasing panels through its rescan pass.
 * <p>
 * The engine is the only authority on array membership: panel blocks have no place or break callbacks, so a
 * panel joins by being reachable from the panel above the engine, and a break that splits the plane must
 * release everything past the split. The panel past the split has to come back once the gap is refilled,
 * so the release cannot be "clear CONNECTED and forget".
 * <p>
 * The rig is built at absolute coordinates in open air rather than inside the test plot, for the reasons
 * documented on {@link SolarPanelExposureTest}.
 */


public class SolarArrayConnectivityTest {
	/** Comfortably above sea level, so the columns are open to the sky. */
	private static final int OPEN_SKY_Y = 80;
	/** Everything between a panel and here is cleared so the panel's column is open to the sky. */
	private static final int COLUMN_TOP_Y = 250;

	/** The engine attaches its array on a 20 tick interval, so give it two of those to settle. */
	private static final int ATTACH_TICKS = 45;
	/** How long the engine may take to notice a change in the plane. One rescan pass plus slack. */
	private static final int REACT_TICKS = 30;

	public static void breakingAPanelReleasesEverythingPastTheSplit(GameTestHelper helper) {
		BlockPos engine = openSkySite(helper);
		BlockPos seed = engine.above();
		BlockPos middle = seed.east();
		BlockPos far = middle.east();

		clear(helper, engine, seed, middle, far);
		clearColumnAbove(helper, seed);
		clearColumnAbove(helper, middle);
		clearColumnAbove(helper, far);

		int sky = helper.getLevel().getBrightness(LightLayer.SKY, seed);
		helper.assertTrue(sky >= 15,
			"the rig site at " + seed + " has no sky access (sky light " + sky + "), so this test cannot measure anything");

		set(helper, engine, EnergyBlocks.ENGINES.get(EngineBlockType.SOLAR).block());
		set(helper, seed, EnergyBlocks.SOLAR_PANEL.block());
		set(helper, middle, EnergyBlocks.SOLAR_PANEL.block());
		set(helper, far, EnergyBlocks.SOLAR_PANEL.block());

		helper.startSequence()
			.thenExecuteAfter(ATTACH_TICKS, () -> {
				assertActivePanels(helper, engine, 3, "a three panel row");
				assertConnected(helper, far, true, "the far end of a three panel row");
			})
			.thenExecute(() -> set(helper, middle, Blocks.AIR))
			.thenExecuteAfter(REACT_TICKS, () -> {
				assertActivePanels(helper, engine, 1, "a row split in the middle");
				assertConnected(helper, seed, true, "the seed panel of a split row");
				assertConnected(helper, far, false, "a panel cut off by the split");
			})
			// And back again, so the release cannot be permanent.
			.thenExecute(() -> set(helper, middle, EnergyBlocks.SOLAR_PANEL.block()))
			.thenExecuteAfter(REACT_TICKS, () -> {
				assertActivePanels(helper, engine, 3, "a row whose gap was refilled");
				assertConnected(helper, far, true, "a panel reconnected through the refilled gap");
			})
			.thenExecute(() -> clear(helper, engine, seed, middle, far))
			.thenSucceed();
	}

	/** The plot's own chunk, but high enough to be above the ocean the plot is buried under. */
	private static BlockPos openSkySite(GameTestHelper helper) {
		BlockPos anchor = helper.absolutePos(new BlockPos(8, 1, 8));
		return new BlockPos(anchor.getX(), OPEN_SKY_Y, anchor.getZ());
	}

	/** Clears everything directly above the panel so its own column is a sky light source. */
	private static void clearColumnAbove(GameTestHelper helper, BlockPos panel) {
		for (int y = panel.getY() + 1; y <= COLUMN_TOP_Y; y++) {
			BlockPos pos = new BlockPos(panel.getX(), y, panel.getZ());
			if (!helper.getLevel().getBlockState(pos).isAir()) {
				helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	private static void set(GameTestHelper helper, BlockPos pos, Block block) {
		helper.getLevel().setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
	}

	/** The rig lives outside the plot, so GameTest will not tear it down for us. */
	private static void clear(GameTestHelper helper, BlockPos... positions) {
		for (BlockPos pos : positions) {
			helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static void assertConnected(GameTestHelper helper, BlockPos panel, boolean expected, String situation) {
		BlockState state = helper.getLevel().getBlockState(panel);
		helper.assertTrue(state.is(EnergyBlocks.SOLAR_PANEL.block()), "the panel vanished from " + panel);
		boolean connected = state.getValue(SolarPanelBlock.CONNECTED);
		helper.assertTrue(connected == expected,
			"expected CONNECTED=" + expected + " for " + situation + ", found " + connected);
	}

	private static void assertActivePanels(GameTestHelper helper, BlockPos enginePos, int expected, String situation) {
		SolarEngineBlockEntity engine = (SolarEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
		helper.assertTrue(engine != null, "no solar engine at " + enginePos);
		helper.assertTrue(engine.activePanels == expected,
			"expected " + expected + " active panel(s) for " + situation + ", the engine counted " + engine.activePanels);
	}
}
