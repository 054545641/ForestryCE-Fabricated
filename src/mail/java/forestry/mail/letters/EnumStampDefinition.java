package forestry.mail.letters;


import forestry.core.platform.item.TwoTintItem;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;


import java.util.function.Function;
import net.minecraft.core.HolderGetter;

public enum EnumStampDefinition implements TwoTintItem.ITwoTintItemSubtype {
	P_1("1n", 1, commonTag("gems/apatite"), TextColor.fromRgb(0x4a8ca7), TextColor.fromRgb(0xffffff)),
	P_2("2n", 2, Items.COPPER_INGOT, TextColor.fromRgb(0xe8c814), TextColor.fromRgb(0xffffff)),
	P_5("5n", 5, commonTag("ingots/tin"), TextColor.fromRgb(0x9c0707), TextColor.fromRgb(0xffffff)),
	P_10("10n", 10, commonTag("ingots/gold"), TextColor.fromRgb(0x7bd1b8), TextColor.fromRgb(0xffffff)),
	P_20("20n", 20, commonTag("gems/diamond"), TextColor.fromRgb(0xff9031), TextColor.fromRgb(0xfff7dd)),
	P_50("50n", 50, commonTag("gems/emerald"), TextColor.fromRgb(0x6431d7), TextColor.fromRgb(0xfff7dd)),
	P_100("100n", 100, Items.NETHER_STAR, TextColor.fromRgb(0xd731ba), TextColor.fromRgb(0xfff7dd)),
	;

	public static final EnumStampDefinition[] VALUES = values();
    private static TagKey<Item> commonTag(String path) { return TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", path)); }

	private final String name;
	private final int primaryColor;
	private final int secondaryColor;
	private final Function<HolderGetter<Item>, Ingredient> craftingIngredient;
	private final int postage;

	EnumStampDefinition(String name, int postage, TagKey<Item> crafting, TextColor primaryColor, TextColor secondaryColor) {
		this(name, postage, items -> Ingredient.of(items.getOrThrow(crafting)), primaryColor, secondaryColor);
	}

	EnumStampDefinition(String name, int postage, Item crafting, TextColor primaryColor, TextColor secondaryColor) {
		this(name, postage, items -> Ingredient.of(crafting), primaryColor, secondaryColor);
	}

	EnumStampDefinition(String name, int postage, Function<HolderGetter<Item>, Ingredient> crafting, TextColor primaryColor, TextColor secondaryColor) {
		this.name = name;
		this.primaryColor = primaryColor.getValue();
		this.secondaryColor = secondaryColor.getValue();
		this.craftingIngredient = crafting;
		this.postage = postage;
	}

	public int getPostage() {
		return this.postage;
	}

	public Ingredient getCraftingIngredient() {
		return getCraftingIngredient(net.minecraft.core.registries.BuiltInRegistries.ITEM);
	}

	public Ingredient getCraftingIngredient(HolderGetter<Item> items) {
		return this.craftingIngredient.apply(items);
	}

	@Override
	public String getSerializedName() {
		return this.name;
	}

	@Override
	public int primaryColor() {
		return this.primaryColor;
	}

	@Override
	public int secondaryColor() {
		return this.secondaryColor;
	}
}
