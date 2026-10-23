package forestry.core.content.decorative;

import forestry.api.core.IBlockSubtype;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;


import javax.annotation.Nullable;
import java.util.Locale;

/**
 * The nineteen big candles, one half block each.
 */
public enum BlockTypeBigCandle implements IBlockSubtype {
	NORMAL(null),
	WHITE(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/white"))),
	LIGHT_GRAY(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/light_gray"))),
	GRAY(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/gray"))),
	BLACK(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/black"))),
	BROWN(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/brown"))),
	RED(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/red"))),
	ORANGE(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/orange"))),
	YELLOW(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/yellow"))),
	LIME(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/lime"))),
	GREEN(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/green"))),
	CYAN(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/cyan"))),
	LIGHT_BLUE(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/light_blue"))),
	BLUE(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/blue"))),
	PURPLE(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/purple"))),
	MAGENTA(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/magenta"))),
	PINK(TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dyes/pink"))),
	REFRACTORY(null),
	RAINBOW(null);

	@Nullable
	private final TagKey<Item> dye;

	BlockTypeBigCandle(@Nullable TagKey<Item> dye) {
		this.dye = dye;
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
