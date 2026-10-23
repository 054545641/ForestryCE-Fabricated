package forestry.core.platform.multiblock.pattern;

import java.util.List;

/**
 * The outcome of {@link MultiblockPattern#validate} (spec 5.2). Either a {@link Match} describing a
 * fully-formed, maximal, fully-loaded structure, or a {@link Failure} listing the failing cells, the
 * first of which drives the player-facing chat message.
 *
 * <p>{@code net.minecraft}-free, like the rest of the pattern layer.
 */
public sealed interface PatternResult permits PatternResult.Match, PatternResult.Failure {

	/** A {@link FailingCell} carrying no message format args. */
	int[] NO_ARGS = new int[0];

	/**
	 * A successful match.
	 *
	 * @param members    Every member position of the structure, lexicographically sorted, lowest first
	 * @param min        The bounding-box minimum corner, recomputed from {@code members} (spec 6.1)
	 * @param max        The bounding-box maximum corner
	 * @param holder     The payload holder and reference coord, the lowest-{@code (x,y,z)} member (spec 6.1)
	 * @param components The member positions mapped to their component type id, for bucketing (spec 8)
	 */
	record Match(
			List<StructurePos> members,
			StructurePos min,
			StructurePos max,
			StructurePos holder,
			List<Component> components
	) implements PatternResult {
	}

	/**
	 * A failed match. {@code cells} is never empty, and {@code cells.get(0)} is the first failing cell, whose
	 * {@link FailingCell#key()} is the parity translation key shown to the player.
	 */
	record Failure(List<FailingCell> cells) implements PatternResult {
		public String firstKey() {
			return this.cells.get(0).key();
		}

		/** The first failing cell, the one that drives the player-facing chat message. */
		public FailingCell first() {
			return this.cells.get(0);
		}
	}

	/** A single member position paired with its component type id. */
	record Component(StructurePos pos, String typeId) {
	}

	/**
	 * A failing cell. It carries its position, the parity translation key explaining why it failed, and any
	 * integer format args the key's lang string consumes. Ex. {@code error.small} takes
	 * {@code minX,minY,minZ}, and {@code error.small.x|y|z} or {@code error.large.x|y|z} takes a single
	 * dimension.
	 *
	 * <p>Content keys whose lang string takes a block or type <em>name</em>, meaning
	 * {@code invalid.interior} and {@code invalid.part}, carry no integer args here. The
	 * {@code net.minecraft}-aware world layer resolves the name from {@link #pos()}.
	 */
	record FailingCell(StructurePos pos, String key, int[] args) {
		public FailingCell(StructurePos pos, String key) {
			this(pos, key, NO_ARGS);
		}
	}
}
