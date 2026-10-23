package forestry.core.content.decorative;

import forestry.api.core.IBlockSubtype;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.MapColor;


import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The twenty-two metal plating blocks. The first six are cast from an ingot, the sixteen after them are
 * lacquered out of any other plating and one dye.
 */
public enum BlockTypeMetalPlating implements IBlockSubtype {
	GOLD(MapColor.GOLD, null),
	IRON(MapColor.METAL, null),
	COPPER(MapColor.COLOR_ORANGE, null),
	TIN(MapColor.METAL, null),
	BRONZE(MapColor.TERRACOTTA_YELLOW, null),
	NETHERITE(MapColor.COLOR_BLACK, null),
	WHITE(MapColor.WOOL, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/white"))),
	LIGHT_GRAY(MapColor.COLOR_LIGHT_GRAY, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/light_gray"))),
	GRAY(MapColor.COLOR_GRAY, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/gray"))),
	BLACK(MapColor.COLOR_BLACK, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/black"))),
	BROWN(MapColor.COLOR_BROWN, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/brown"))),
	RED(MapColor.COLOR_RED, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/red"))),
	ORANGE(MapColor.COLOR_ORANGE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/orange"))),
	YELLOW(MapColor.COLOR_YELLOW, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/yellow"))),
	LIME(MapColor.COLOR_LIGHT_GREEN, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/lime"))),
	GREEN(MapColor.COLOR_GREEN, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/green"))),
	CYAN(MapColor.COLOR_CYAN, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/cyan"))),
	BLUE(MapColor.COLOR_BLUE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/blue"))),
	LIGHT_BLUE(MapColor.COLOR_LIGHT_BLUE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/light_blue"))),
	PURPLE(MapColor.COLOR_PURPLE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/purple"))),
	MAGENTA(MapColor.COLOR_MAGENTA, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/magenta"))),
	PINK(MapColor.COLOR_PINK, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/pink")));

	private final MapColor color;
	@Nullable
	private final TagKey<Item> dye;

	// Deviation from 1.20.1: the map colors lived in a static HashMap built fresh on every call there, and
	// the dye tags in a second one. Both are fields here, so the recipe loop can walk values() in
	// declaration order rather than a hash order that datagen would write differently between runs
	BlockTypeMetalPlating(MapColor color, @Nullable TagKey<Item> dye) {
		this.color = color;
		this.dye = dye;
	}

	/**
	 * @return The map color the block carries
	 */
	public MapColor getMapColor() {
		return this.color;
	}

	/**
	 * @return The dye tag the lacquering recipe takes, or null where the plating is cast from an ingot
	 */
	@Nullable
	public TagKey<Item> getDye() {
		return this.dye;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Ex. {@code RED} -> {@code "red_metal_plating"}
	 *
	 * @return The name the recipe id is built from, which reads colour first
	 */
	public String getName() {
		return getSerializedName() + "_metal_plating";
	}
}
