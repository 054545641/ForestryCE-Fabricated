package forestry.fabric.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import forestry.api.core.multiblock.IMultiblockComponent;
import forestry.api.core.multiblock.IMultiblockController;
import forestry.api.core.multiblock.IMultiblockInventoryProbe;
import forestry.apiculture.alveary.AlvearyBlock;
import forestry.apiculture.features.ApicultureBlocks;
import forestry.agriculture.multifarm.blocks.MultifarmBlockType;
import forestry.agriculture.multifarm.blocks.MultifarmMaterialType;
import forestry.agriculture.features.MultifarmBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Engine-agnostic mechanics shared by the multiblock inventory-conservation GameTests ({@link MultiblockGameTests}).
 *
 * <p>Everything here is built on vanilla / Forestry-public APIs so the SAME tests run against the current
 * ("Erogenous Beef") engine and the upcoming multiblock overhaul. The only engine-specific coupling is reading the
 * shared inventory, and that goes through the explicit {@link IMultiblockInventoryProbe} seam — never through engine
 * internals. Structures are built block-by-block (no shipped structure NBT beyond an empty arena), inventory is read
 * through the probe, drops are counted as real {@link ItemEntity}s, and a reload is a genuine block-entity teardown +
 * recreate whose reform is driven by the engine's own server tick (no registry calls).
 */
public final class MultiblockTestSupport {
	/** Slot count the alveary's shared bee inventory surfaces as a {@link Container} when assembled. */
	public static final int ALVEARY_INV_SIZE = 9;
	/** Slot count the farm's shared inventory surfaces as a {@link Container} when assembled. */
	public static final int FARM_INV_SIZE = 22;

	private MultiblockTestSupport() {
	}

	/* ===================== structure building (relative coords; returns absolute member positions) ===================== */

	/**
	 * Builds a minimal valid 3x3x3 alveary anchored at {@code base} (relative), capped with a 3x3 oak-slab layer.
	 * The surrounding GameTest arena is air, satisfying the "space around the entrances" rule.
	 *
	 * @return the absolute world positions of the 27 alveary block entities (sorted), for later teardown/inventory access.
	 */
	public static List<BlockPos> buildAlveary(GameTestHelper helper, BlockPos base) {
		BlockState plain = ApicultureBlocks.ALVEARY.get(AlvearyBlock.Type.PLAIN).defaultState();
		BlockState slab = Blocks.OAK_SLAB.defaultBlockState();
		List<BlockPos> members = new ArrayList<>();
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				for (int y = 0; y < 3; y++) {
					BlockPos rel = base.offset(x, y, z);
					helper.setBlock(rel, plain);
					members.add(helper.absolutePos(rel));
				}
			}
		}
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				helper.setBlock(base.offset(x, 3, z), slab); // slab cap at maxY + 1
			}
		}
		members.sort(BlockPos::compareTo);
		return members;
	}

	/**
	 * Builds a minimal valid 3x3x4 farm anchored at {@code base} (relative): all plain blocks with a single gearbox at
	 * the bottom centre (a level-0 exterior position, which the band/interior rules permit).
	 *
	 * @return the absolute world positions of the 36 farm block entities (sorted).
	 */
    public static List<BlockPos> buildFarm(GameTestHelper helper, BlockPos base) { return FarmBuilder.build(helper, base); }
    // Resolving farm enum interfaces is deferred until the optional farms module is actually exercised.
    private static final class FarmBuilder {
	static List<BlockPos> build(GameTestHelper helper, BlockPos base) {
		BlockState plain = MultifarmBlocks.FARM.get(MultifarmBlockType.PLAIN, MultifarmMaterialType.STONE_BRICK).defaultState();
		BlockState gearbox = MultifarmBlocks.FARM.get(MultifarmBlockType.GEARBOX, MultifarmMaterialType.STONE_BRICK).defaultState();
		List<BlockPos> members = new ArrayList<>();
		for (int x = 0; x < 3; x++) {
			for (int z = 0; z < 3; z++) {
				for (int y = 0; y < 4; y++) {
					BlockPos rel = base.offset(x, y, z);
					// gearbox at bottom centre, plain everywhere else
					helper.setBlock(rel, (x == 1 && y == 0 && z == 1) ? gearbox : plain);
					members.add(helper.absolutePos(rel));
				}
			}
		}
		members.sort(BlockPos::compareTo);
		return members;
	}

    }

	/* ===================== controller / inventory access (engine-agnostic) ===================== */

	/** Resolves the controller that owns the member at {@code memberAbs}, or null if that block is not a connected part. */
	public static IMultiblockController controllerAt(ServerLevel level, BlockPos memberAbs) {
		if (level.getBlockEntity(memberAbs) instanceof IMultiblockComponent component) {
			return component.getMultiblockLogic().getController();
		}
		return null;
	}

	public static boolean isAssembled(ServerLevel level, BlockPos memberAbs) {
		IMultiblockController controller = controllerAt(level, memberAbs);
		return controller != null && controller.isAssembled();
	}

	/**
	 * Reads the shared inventory through {@link IMultiblockInventoryProbe} — the one engine-agnostic seam. Returns an
	 * empty list if the member is unattached or its controller does not implement the probe.
	 */
	public static List<ItemStack> snapshot(ServerLevel level, BlockPos memberAbs) {
		IMultiblockController controller = controllerAt(level, memberAbs);
		if (controller instanceof IMultiblockInventoryProbe probe) {
			return probe.snapshotSharedInventory();
		}
		return List.of();
	}

	/** Item -> total count, for order-independent inventory comparison. */
	public static Map<Item, Integer> tally(List<ItemStack> stacks) {
		Map<Item, Integer> tally = new TreeMap<>((a, b) -> Integer.compare(BuiltInRegistries.ITEM.getId(a), BuiltInRegistries.ITEM.getId(b)));
		for (ItemStack stack : stacks) {
			tally.merge(stack.getItem(), stack.getCount(), Integer::sum);
		}
		return tally;
	}

	public static int total(Map<Item, Integer> tally) {
		int sum = 0;
		for (int count : tally.values()) {
			sum += count;
		}
		return sum;
	}

	/**
	 * Writes {@code stack} into one slot of the machine's shared inventory through the vanilla {@link Container} surface
	 * (every assembled member delegates to the single shared inventory), bypassing slot-acceptance rules — we only need
	 * something countable to track. Must be called AFTER assembly. Throws if no shared inventory of {@code invSize} is
	 * found among the members.
	 */
	public static void insertItem(ServerLevel level, List<BlockPos> membersAbs, int invSize, int slot, ItemStack stack) {
		for (BlockPos member : membersAbs) {
			if (level.getBlockEntity(member) instanceof Container container && container.getContainerSize() == invSize) {
				container.setItem(slot, stack.copy());
				container.setChanged();
				return;
			}
		}
		throw new IllegalStateException("no shared inventory of size " + invSize + " found among members (not assembled?)");
	}

	/* ===================== drops ===================== */

}
