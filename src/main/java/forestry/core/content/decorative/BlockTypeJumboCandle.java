package forestry.core.content.decorative;

import forestry.api.core.IBlockSubtype;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.MapColor;


import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The nineteen jumbo candles, one full block each.
 */
public enum BlockTypeJumboCandle implements IBlockSubtype {
	NORMAL(MapColor.SAND, null),
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
	LIGHT_BLUE(MapColor.COLOR_LIGHT_BLUE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/light_blue"))),
	BLUE(MapColor.COLOR_BLUE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/blue"))),
	PURPLE(MapColor.COLOR_PURPLE, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/purple"))),
	MAGENTA(MapColor.COLOR_MAGENTA, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/magenta"))),
	PINK(MapColor.COLOR_PINK, TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/pink"))),
	REFRACTORY(MapColor.COLOR_RED, null),
	RAINBOW(MapColor.COLOR_MAGENTA, null);

	private final MapColor color;
	@Nullable
	private final TagKey<Item> dye;

	BlockTypeJumboCandle(MapColor color, @Nullable TagKey<Item> dye) {
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
	 * @return The dye tag the dyeing recipe takes, or null where the candle is made from wax
	 */
	@Nullable
	public TagKey<Item> getDye() {
		return this.dye;
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ENGLISH);
	}
}
