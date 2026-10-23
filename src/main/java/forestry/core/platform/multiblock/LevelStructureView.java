package forestry.core.platform.multiblock;

import forestry.core.platform.multiblock.pattern.StructurePos;
import forestry.core.platform.multiblock.pattern.StructureView;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The Phase-2 world adapter, a {@link StructureView} over a Forge {@link Level} (plan Task 2.1, spec 5.2).
 * It performs <b>chunk-safe</b> reads, because {@link #isLoaded} checks the chunk source rather than forcing
 * a load. That lets the pattern engine's maximality and loaded-shell rule (spec 5.2, audit D1) defer on
 * partially-loaded structures instead of assembling a shrunken sub-prism.
 *
 * <p>{@link #sample} reads the in-world {@link BlockEntity}, the blockstate's
 * {@code is(BlockTags.WOODEN_SLABS)} (the alveary slab cap), and {@code isSolidRender} (the alveary
 * entrance air ring), and resolves the cell's {@code componentTypeId} from the BE itself.
 *
 * <p><b>Type-id translation.</b> See {@link #typeIdFor}. A BE that is not a member of either machine maps
 * to {@code null} (the cell is reported as not-a-component -> {@code invalid.interior}).
 */
public final class LevelStructureView implements StructureView {
	private final Level level;

	public LevelStructureView(Level level) {
		this.level = level;
	}

	/* ===== StructurePos <-> BlockPos ===== */

	private static BlockPos toBlockPos(StructurePos pos) {
		return new BlockPos(pos.x(), pos.y(), pos.z());
	}

	@Override
	public boolean isLoaded(StructurePos pos) {
		// Chunk-safe, do NOT force-load. hasChunk mirrors the old engine's chunk checks
		return this.level instanceof net.minecraft.server.level.ServerLevel server
            ? server.getChunkSource().getChunkNow(pos.x() >> 4, pos.z() >> 4) != null
            : this.level.getChunkSource().hasChunk(pos.x() >> 4, pos.z() >> 4);
	}

	@Override
	public CellSample sample(StructurePos pos) {
		BlockPos blockPos = toBlockPos(pos);

		// Resolve the component via the BlockEntity. getBlockEntity is safe here because validation
		// only samples cells it has already confirmed loaded (loaded-shell rule).
        var chunk = this.level instanceof net.minecraft.server.level.ServerLevel server
            ? server.getChunkSource().getChunkNow(pos.x() >> 4, pos.z() >> 4) : this.level.getChunkAt(blockPos);
        if (chunk == null) throw new IllegalStateException("Structure sampled an unavailable chunk: " + blockPos);
		BlockEntity be = chunk.getBlockEntity(blockPos);
		String componentTypeId = be == null ? null : typeIdFor(be);
		boolean isComponent = componentTypeId != null;

		BlockState state = chunk.getBlockState(blockPos);
		boolean isWoodenSlab = state.is(BlockTags.WOODEN_SLABS);
		boolean isSolidRender = state.isSolidRender();

		return new CellSample(isComponent, be, isWoodenSlab, isSolidRender, componentTypeId);
	}

	/* ===== BlockEntity -> pattern type-id mapping ===== */

	/**
	 * Resolves a cell's pattern type-id from its block entity.
	 *
	 * <p>The part declares its own role through
	 * {@link MultiblockTileEntityForestry#patternTypeId}, so this class names no part class and no
	 * machine. A subclass that does not override it inherits its parent's role, which keeps the
	 * pre-rewrite behavior that an addon subclassing a role part is that role.
	 *
	 * <p>Every part that is not a distinguished role returns the machine's generic {@code *_part}. The
	 * pattern layer never tells those apart, because {@code Predicates} only compares {@code PLAIN} and
	 * {@code GEARBOX} by equality and {@code PREFIX} by prefix, so a per-part id would carry no
	 * information.
	 *
	 * <p>Membership requires {@link MultiblockTileEntityForestry}, because the rest of the engine does.
	 * {@code MultiblockValidation.assemble} resolves the holder (the lowest member) as one and returns
	 * otherwise, and anchoring and stash-clearing skip anything else. A part the engine cannot anchor
	 * would make a structure fail to form with no error. Addon parts therefore extend
	 * {@code TileAlveary} or {@code TileFarm}, as they did before the rewrite.
	 */
	@Nullable
	static String typeIdFor(BlockEntity be) {
		return be instanceof MultiblockTileEntityForestry<?> component ? component.patternTypeId() : null;
	}
}
