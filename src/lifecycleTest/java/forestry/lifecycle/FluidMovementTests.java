package forestry.lifecycle;

import forestry.core.platform.fluids.ForestryFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;

/** Exercises the player's real fluid detection, current and travel paths. */
final class FluidMovementTests {

	static void playerMovement(GameTestHelper helper) {
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4));
		for (ForestryFluids fluid : ForestryFluids.values()) {
			fillPool(helper, center, fluid, false);
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			Vec3 start = Vec3.atBottomCenterOf(center);
			player.setPos(start);
			player.baseTick();
			helper.assertTrue(player.isInFluidType(fluid.getFluid().getFluidType()), fluid + " is detected by player physics");
			helper.assertTrue(!player.isInWater(), fluid + " keeps its non-water interactions");
			player.travel(new Vec3(0, 0, 1));
			helper.assertTrue(player.getZ() > start.z + 0.01, fluid + " allows movement from player input");
			helper.assertTrue(player.getDeltaMovement().y < 0, fluid + " applies gravity");

			player.setPos(start);
			player.setDeltaMovement(0.2, 0, 0);
			player.baseTick();
			player.travel(Vec3.ZERO);
			helper.assertTrue(player.getX() > start.x + 0.1, fluid + " applies existing momentum to position");
			helper.assertTrue(player.getDeltaMovement().x > 0 && player.getDeltaMovement().x < 0.2,
				fluid + " damps existing momentum");

			player.setPos(start);
			player.setDeltaMovement(Vec3.ZERO);
			player.baseTick();
			player.jumpInFluid(fluid.getFluid().getFluidType());
			player.travel(Vec3.ZERO);
			helper.assertTrue(player.getY() > start.y, fluid + " allows swimming upwards");
		}
		helper.succeed();
	}

	static void honeyCurrentRelease(GameTestHelper helper) {
		BlockPos center = helper.absolutePos(new BlockPos(4, 2, 4));
		fillPool(helper, center, ForestryFluids.HONEY, true);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Vec3 start = Vec3.atBottomCenterOf(center);
		Vec3 current = helper.getLevel().getFluidState(center).getFlow(helper.getLevel(), center);
		helper.assertTrue(current.x > 0.9, "The honey pool has an actual flowing current");
		double maxSpeed = 0;
		double minDisplacement = Double.POSITIVE_INFINITY;
		for (int tick = 0; tick < 600; tick++) {
			// Keep sampling the same current for 30 seconds of physics without resetting velocity.
			player.setPos(start);
			player.baseTick();
			player.travel(Vec3.ZERO);
			maxSpeed = Math.max(maxSpeed, player.getDeltaMovement().horizontalDistance());
			minDisplacement = Math.min(minDisplacement, player.getX() - start.x);
		}

		BlockPos source = center.west();
		var sourceState = helper.getLevel().getBlockState(source);
		var bucket = ((LiquidBlock) sourceState.getBlock()).pickupBlock(player, helper.getLevel(), source, sourceState);
		helper.assertTrue(bucket.is(ForestryFluids.HONEY.getBucket()), "The honey source can be collected in a bucket");
		// Remove the remaining flow, modelling the moment the collected honey finishes draining.
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
			helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		}
		player.setPos(start);
		player.baseTick();
		helper.assertTrue(!player.isInFluidType(), "The player has left the drained honey");
		player.travel(Vec3.ZERO);
		helper.assertTrue(player.position().subtract(start).horizontalDistance() < 0.2,
			"Collecting honey must not launch the player after prolonged exposure to its current");
		helper.assertTrue(maxSpeed < 0.2, "Flowing honey velocity stays bounded throughout prolonged exposure");
		helper.assertTrue(minDisplacement > 0.001, "Flowing honey moves the player instead of storing its force");
		helper.succeed();
	}

	private static void fillPool(GameTestHelper helper, BlockPos center, ForestryFluids fluid, boolean flowing) {
		var block = fluid.getFeature().fluidBlock().block();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
			helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
			helper.getLevel().setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
			int level = flowing ? pos.getX() - center.getX() + 1 : 0;
			helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState().setValue(LiquidBlock.LEVEL, level));
		}
	}
}
