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
 * Covers a solar array noticing that one of its panels has been shaded.
 * <p>
 * A panel's {@code IN_DAYLIGHT} state is what the engine counts to decide how much power to make, so it has to track
 * the sky in something close to real time. It used to be refreshed only from {@code SolarPanelBlock.randomTick}, which
 * for any one block averages {@code 4096 / randomTickSpeed} ticks — about 68 seconds at the default speed, with no
 * upper bound. Building a roof over an array therefore kept paying out for a minute or more, and because the engine's
 * count was maintained by incrementing and decrementing rather than recounting, any missed update stayed wrong.
 * <p>
 * The rig is built at absolute coordinates in open air rather than inside the test plot. GameTest buries its plots
 * deep underground, with no sky access at all, and on 1.20.1 the plot sits under an ocean so a shaft dug up to the
 * surface simply refills with water and blocks sky light. Building above sea level and clearing the panel's own
 * column sidesteps whatever terrain the plot happens to be under. The rig is torn down again at the end.
 */


public class SolarPanelExposureTest {
	/** Comfortably above sea level, so the column is open to the sky. */
	private static final int OPEN_SKY_Y = 80;
	/** Everything between the panel and here is cleared so the panel's column is open to the sky. */
	private static final int COLUMN_TOP_Y = 250;

	/** The engine attaches its array on a 20 tick interval, so give it two of those to settle. */
	private static final int ATTACH_TICKS = 45;
	/** How long the engine may take to notice a change in the sky. One rescan interval plus slack. */
	private static final int REACT_TICKS = 30;

	public static void shadingAPanelStopsItCountingAsActive(GameTestHelper helper) {
		BlockPos engine = openSkySite(helper);
		BlockPos panel = engine.above();
		// Well clear of the panel, to prove the check is "is anything above me" and not "is a block touching me".
		BlockPos roof = panel.above(5);

		clear(helper, engine, panel, roof);
		// GameTest hands out a different plot column depending on how many tests are registered, so the rig can land
		// under terrain. Open the column above it rather than depending on where it happens to be.
		clearColumnAbove(helper, panel);

		int sky = helper.getLevel().getBrightness(LightLayer.SKY, panel);
		helper.assertTrue(sky >= 15,
			"the rig site at " + panel + " has no sky access (sky light " + sky + "), so this test cannot measure anything");

		set(helper, engine, EnergyBlocks.ENGINES.get(EngineBlockType.SOLAR).block());
		set(helper, panel, EnergyBlocks.SOLAR_PANEL.block());

		helper.startSequence()
			.thenExecuteAfter(ATTACH_TICKS, () -> {
				assertLit(helper, panel, true, "an unobstructed panel");
				assertActivePanels(helper, engine, 1, "an unobstructed panel");
			})
			.thenExecute(() -> set(helper, roof, Blocks.STONE))
			.thenExecuteAfter(REACT_TICKS, () -> {
				assertLit(helper, panel, false, "a panel with stone 5 blocks above it");
				assertActivePanels(helper, engine, 0, "a panel with stone 5 blocks above it");
			})
			// And back again, so the fix cannot be "always report obscured".
			.thenExecute(() -> set(helper, roof, Blocks.AIR))
			.thenExecuteAfter(REACT_TICKS, () -> {
				assertLit(helper, panel, true, "a panel whose roof was removed");
				assertActivePanels(helper, engine, 1, "a panel whose roof was removed");
			})
			.thenExecute(() -> clear(helper, engine, panel, roof))
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

	private static void assertLit(GameTestHelper helper, BlockPos panel, boolean expected, String situation) {
		BlockState state = helper.getLevel().getBlockState(panel);
		helper.assertTrue(state.is(EnergyBlocks.SOLAR_PANEL.block()), "the panel vanished from " + panel);
		boolean lit = state.getValue(SolarPanelBlock.IN_DAYLIGHT);
		helper.assertTrue(lit == expected,
			"expected IN_DAYLIGHT=" + expected + " for " + situation + ", found " + lit);
	}

	private static void assertActivePanels(GameTestHelper helper, BlockPos enginePos, int expected, String situation) {
		SolarEngineBlockEntity engine = (SolarEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
		helper.assertTrue(engine != null, "no solar engine at " + enginePos);
		helper.assertTrue(engine.activePanels == expected,
			"expected " + expected + " active panel(s) for " + situation + ", the engine counted " + engine.activePanels);
	}
}
