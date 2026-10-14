package forestry.api.lepidopterology;

import net.minecraft.resources.Identifier;

import java.util.List;

import static forestry.api.ForestryConstants.forestry;

/**
 * All butterfly species registered by base Forestry.
 */
public class ForestryButterflySpecies {
	// Butterflies
	public static final Identifier CABBAGE_WHITE = forestry("cabbage_white");
	public static final Identifier BRIMSTONE = forestry("brimstone");
	public static final Identifier AURORA = forestry("aurora");
	public static final Identifier CLOUDED_YELLOW = forestry("clouded_yellow");
	public static final Identifier PALAENO_SULPHUR = forestry("palaeno_sulphur");
	public static final Identifier RESEDA = forestry("reseda");
	public static final Identifier SPRING_AZURE = forestry("spring_azure");
	public static final Identifier GOZORA_AZURE = forestry("gozora_azure");
	public static final Identifier CITRUS_SWALLOWTAIL = forestry("citrus_swallow");
	public static final Identifier EMERALD_PEACOCK = forestry("emerald_peacock");
	public static final Identifier THOAS_SWALLOWTAIL = forestry("thoas_swallow");
	public static final Identifier SPICEBUSH_SWALLOWTAIL = forestry("spicebush_swallow");
	public static final Identifier BLACK_SWALLOWTAIL = forestry("black_swallow");
	public static final Identifier ZEBRA_SWALLOWTAIL = forestry("zebra_swallow");
	public static final Identifier GLASSWING = forestry("glasswing");
	public static final Identifier SPECKLED_WOOD = forestry("speckled_wood");
	public static final Identifier MADEIRAN_SPECKLED_WOOD = forestry("mspeckled_wood");
	public static final Identifier CANARY_SPECKLED_WOOD = forestry("cspeckled_wood");
	public static final Identifier MENELAUS_BLUE_MORPHO = forestry("mbluemorpho");
	public static final Identifier PELEIDES_BLUE_MORPHO = forestry("pbluemorpho");
	public static final Identifier RHETENOR_BLUE_MORPHO = forestry("rbluemorpho");
	public static final Identifier COMMA = forestry("comma");
	public static final Identifier BATESIA = forestry("batesia");
	public static final Identifier BLUE_WING = forestry("blue_wing");
	public static final Identifier MONARCH = forestry("monarch");
	public static final Identifier BLUE_DUKE = forestry("blue_duke");
	public static final Identifier GLASSY_TIGER = forestry("glassy_tiger");
	public static final Identifier POSTMAN = forestry("postman");
	public static final Identifier MALACHITE = forestry("malachite");
	public static final Identifier LEOPARD_LACEWING = forestry("leopard_lacewing");
	public static final Identifier DIANA_FRITILLARY = forestry("diana_fritillary");

	// Moths
	public static final Identifier BRIMSTONE_MOTH = forestry("brimstone_moth");
	public static final Identifier LATTICED_HEATH = forestry("latticed_heath");
	public static final Identifier ATLAS = forestry("atlas");
	public static final Identifier BOMBYX_MORI = forestry("bombyx_mori");

	/**
	 * All built-in butterfly species ids, in declaration order. Compile-time constant list (not a view of the
	 * reloadable species registry), safe to iterate at client-registration time.
	 */
	public static final List<Identifier> ALL = List.of(
		CABBAGE_WHITE, BRIMSTONE, AURORA, CLOUDED_YELLOW, PALAENO_SULPHUR, RESEDA, SPRING_AZURE, GOZORA_AZURE,
		CITRUS_SWALLOWTAIL, EMERALD_PEACOCK, THOAS_SWALLOWTAIL, SPICEBUSH_SWALLOWTAIL, BLACK_SWALLOWTAIL,
		ZEBRA_SWALLOWTAIL, GLASSWING, SPECKLED_WOOD, MADEIRAN_SPECKLED_WOOD, CANARY_SPECKLED_WOOD,
		MENELAUS_BLUE_MORPHO, PELEIDES_BLUE_MORPHO, RHETENOR_BLUE_MORPHO, COMMA, BATESIA, BLUE_WING, MONARCH,
		BLUE_DUKE, GLASSY_TIGER, POSTMAN, MALACHITE, LEOPARD_LACEWING, DIANA_FRITILLARY,
		BRIMSTONE_MOTH, LATTICED_HEATH, ATLAS, BOMBYX_MORI
	);
}
