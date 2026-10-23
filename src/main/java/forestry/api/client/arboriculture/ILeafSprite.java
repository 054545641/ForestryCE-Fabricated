package forestry.api.client.arboriculture;

import net.minecraft.resources.Identifier;

/**
 * Provides textures used for rendering leaves on a Forestry tree.
 */
public interface ILeafSprite {
	/**
	 * The sprite used when no leaf sprite is registered for a species.
	 *
	 * @since 2.10.0
	 */
	ILeafSprite MISSING = new ILeafSprite() {
		private static final Identifier LOCATION = Identifier.withDefaultNamespace("missingno");

		@Override
		public Identifier get(boolean pollinated, boolean fancy) {
			return LOCATION;
		}

		@Override
		public Identifier getParticle() {
			return LOCATION;
		}
	};

	/**
	 * Returns the location of the leaf texture sprite to use for rendering.
	 *
	 * @param pollinated Whether the leaves are pollinated.
	 * @param fancy      Whether the game is using fancy graphics. If fast, replace transparent pixels with black.
	 * @return The location of the sprite to use for leaf block rendering.
	 */
	Identifier get(boolean pollinated, boolean fancy);

	/**
	 * @return The leaf texture used for block particles (walking, destroying, etc.)
	 */
	Identifier getParticle();
}
