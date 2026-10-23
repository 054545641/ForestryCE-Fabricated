package forestry.arboriculture.leaves;

/**
 * A leaf block whose species is fixed by a {@link ForestryLeafType} rather than carried in per-stack NBT.
 * Implemented by the tile-less genetic leaves ({@link DefaultLeavesBlock}, {@link DefaultFruitLeavesBlock}) so a
 * shared item class can resolve their species for display name and tint. {@link DecorativeLeavesBlock} resolves the
 * same way via its own {@code ItemBlockDecorativeLeaves}.
 */
public interface ILeafTypeBlock {
	ForestryLeafType getType();
}
