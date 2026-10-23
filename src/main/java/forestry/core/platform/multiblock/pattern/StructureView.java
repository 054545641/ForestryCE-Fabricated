package forestry.core.platform.multiblock.pattern;

/**
 * A Minecraft-free read-only view of a region of the world, consulted by {@link MultiblockPattern}
 * during validation (spec 5.2). The real implementation, {@code LevelStructureView} in Phase 2, wraps
 * a Forge {@code Level} and performs chunk-safe reads. Unit tests use {@code FakeStructureView}.
 *
 * <p>No {@code net.minecraft} types appear here on purpose. See {@link StructurePos}.
 */
public interface StructureView {
	/**
	 * Samples the cell at the given position. Implementations must return a non-null {@link CellSample}
	 * even for air or unrecognised blocks, with {@code isComponent == false}.
	 */
	CellSample sample(StructurePos pos);

	/**
	 * Determines whether the chunk containing the given position is currently loaded. The maximality and
	 * loaded-shell rule (spec 5.2) requires every consulted cell to be loaded before a match can be confirmed.
	 */
	boolean isLoaded(StructurePos pos);

	/**
	 * An immutable snapshot of a single cell, exposing only the information the predicates need.
	 *
	 * @param isComponent     Whether this cell holds a Forestry multiblock component (BE)
	 * @param component       The backing object, a BE in-world or a test stand-in. May be {@code null}
	 * @param isWoodenSlab    Whether the block is in {@code BlockTags.WOODEN_SLABS} (alveary slab cap)
	 * @param isSolidRender   Whether the block renders solid (alveary entrance air-ring check)
	 * @param componentTypeId A stable type id for the component, {@code null} when
	 *                        {@code isComponent == false}. Ex. {@code "alveary_plain"},
	 *                        {@code "farm_gearbox"}
	 */
	record CellSample(
			boolean isComponent,
			Object component,
			boolean isWoodenSlab,
			boolean isSolidRender,
			String componentTypeId
	) {
	}
}
