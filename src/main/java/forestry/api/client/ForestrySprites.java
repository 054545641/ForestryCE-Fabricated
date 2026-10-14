package forestry.api.client;

import forestry.api.ForestryConstants;
import net.minecraft.resources.Identifier;

/**
 * All sprites loaded by forestry for use in the {@link ITextureManager}.
 * SLOT_* sprites are also available on the vanilla GUI atlas.
 */
public class ForestrySprites {
	/**
	 * Texture owned by the atlas manager; GUI extraction draws its sprites with an explicit render pipeline.
	 */
	public static final Identifier TEXTURE_ATLAS = ForestryConstants.forestry("textures/atlas/gui.png");

	public static final Identifier HABITAT_DESERT = ForestryConstants.forestry("habitats/desert");
	public static final Identifier HABITAT_END = ForestryConstants.forestry("habitats/end");
	public static final Identifier HABITAT_FOREST = ForestryConstants.forestry("habitats/forest");
	public static final Identifier HABITAT_HILLS = ForestryConstants.forestry("habitats/hills");
	public static final Identifier HABITAT_JUNGLE = ForestryConstants.forestry("habitats/jungle");
	public static final Identifier HABITAT_MUSHROOM = ForestryConstants.forestry("habitats/mushroom");
	public static final Identifier HABITAT_NETHER = ForestryConstants.forestry("habitats/nether");
	public static final Identifier HABITAT_OCEAN = ForestryConstants.forestry("habitats/ocean");
	public static final Identifier HABITAT_PLAINS = ForestryConstants.forestry("habitats/plains");
	public static final Identifier HABITAT_SNOW = ForestryConstants.forestry("habitats/snow");
	public static final Identifier HABITAT_SWAMP = ForestryConstants.forestry("habitats/swamp");
	public static final Identifier HABITAT_TAIGA = ForestryConstants.forestry("habitats/taiga");
	public static final Identifier MISC_ACCESS_SHARED = ForestryConstants.forestry("misc/access.shared");
	public static final Identifier MISC_ENERGY = ForestryConstants.forestry("misc/energy");
	public static final Identifier MISC_HINT = ForestryConstants.forestry("misc/hint");
	public static final Identifier ANALYZER_ANYTHING = ForestryConstants.forestry("analyzer/anything");
	public static final Identifier ANALYZER_BEE = ForestryConstants.forestry("analyzer/bee");
	public static final Identifier ANALYZER_CAVE = ForestryConstants.forestry("analyzer/cave");
	public static final Identifier ANALYZER_CLOSED = ForestryConstants.forestry("analyzer/closed");
	public static final Identifier ANALYZER_DRONE = ForestryConstants.forestry("analyzer/drone");
	public static final Identifier ANALYZER_FLYER = ForestryConstants.forestry("analyzer/flyer");
	public static final Identifier ANALYZER_ITEM = ForestryConstants.forestry("analyzer/item");
	public static final Identifier ANALYZER_NOCTURNAL = ForestryConstants.forestry("analyzer/nocturnal");
	public static final Identifier ANALYZER_PRINCESS = ForestryConstants.forestry("analyzer/princess");
	public static final Identifier ANALYZER_PURE_BREED = ForestryConstants.forestry("analyzer/pure_breed");
	public static final Identifier ANALYZER_PURE_CAVE = ForestryConstants.forestry("analyzer/pure_cave");
	public static final Identifier ANALYZER_PURE_FLYER = ForestryConstants.forestry("analyzer/pure_flyer");
	public static final Identifier ANALYZER_PURE_NOCTURNAL = ForestryConstants.forestry("analyzer/pure_nocturnal");
	public static final Identifier ANALYZER_QUEEN = ForestryConstants.forestry("analyzer/queen");
	public static final Identifier ANALYZER_TREE = ForestryConstants.forestry("analyzer/tree");
	public static final Identifier ANALYZER_SAPLING = ForestryConstants.forestry("analyzer/tree_sapling");
	public static final Identifier ANALYZER_POLLEN = ForestryConstants.forestry("analyzer/pollen");
	public static final Identifier ANALYZER_FLUTTER = ForestryConstants.forestry("analyzer/flutter");
	public static final Identifier ANALYZER_BUTTERFLY = ForestryConstants.forestry("analyzer/butterfly");
	public static final Identifier ANALYZER_SERUM = ForestryConstants.forestry("analyzer/serum");
	public static final Identifier ANALYZER_CATERPILLAR = ForestryConstants.forestry("analyzer/caterpillar");
	public static final Identifier ANALYZER_COCOON = ForestryConstants.forestry("analyzer/cocoon");
	public static final Identifier ANALYZER_BEE_FERTILITY = ForestryConstants.forestry("analyzer/bee_fertility");
	public static final Identifier ANALYZER_BUTTERFLY_FERTILITY = ForestryConstants.forestry("analyzer/butterfly_fertility");
	public static final Identifier ANALYZER_TOLERANCE_NONE = ForestryConstants.forestry("analyzer/tolerance_none");
	public static final Identifier ANALYZER_TOLERANCE_UP = ForestryConstants.forestry("analyzer/tolerance_up");
	public static final Identifier ANALYZER_TOLERANCE_DOWN = ForestryConstants.forestry("analyzer/tolerance_down");
	public static final Identifier ANALYZER_TOLERANCE_BOTH = ForestryConstants.forestry("analyzer/tolerance_both");
	public static final Identifier ERROR_ERRORED = ForestryConstants.forestry("errors/errored");
	public static final Identifier ERROR_UNKNOWN = ForestryConstants.forestry("errors/unknown");
	public static final Identifier SLOT_BLOCKED = ForestryConstants.forestry("slots/blocked");
	public static final Identifier SLOT_BLOCKED_2 = ForestryConstants.forestry("slots/blocked_2");
	public static final Identifier SLOT_LIQUID = ForestryConstants.forestry("slots/liquid");
	public static final Identifier SLOT_CONTAINER = ForestryConstants.forestry("slots/container");
	public static final Identifier SLOT_LOCKED = ForestryConstants.forestry("slots/locked");
	public static final Identifier SLOT_COCOON = ForestryConstants.forestry("slots/cocoon");
	public static final Identifier SLOT_BEE = ForestryConstants.forestry("slots/bee");
	public static final Identifier MAIL_CARRIER_PLAYER = ForestryConstants.forestry("mail/carrier.player");
	public static final Identifier MAIL_CARRIER_TRADER = ForestryConstants.forestry("mail/carrier.trader");
}
