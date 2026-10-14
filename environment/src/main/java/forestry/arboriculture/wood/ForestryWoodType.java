package forestry.arboriculture.wood;

/** Probe registry fixture: production enum names/order, without the genetics/wood registries. */
public enum ForestryWoodType {
    LARCH, TEAK, CAMELTHORN, LIME, CHESTNUT, WENGE, BAOBAB, SEQUOIA, KAPOK, EBONY, ELM, MAHOGANY, BALSA, WILLOW, WALNUT, GREENHEART, SOUR_CHERRY, MAHOE, POPLAR, PALM, PAPAYA, PINE, PLUM, MAPLE, LEMON, GIANT_SEQUOIA, IPE, PADAUK, COCOBOLO, FIR, COCONUT, BEECH, FEIJOA, DOGWOOD, GINKGO, JACARANDA, PEWEN, MACROCARPA, OLIVE, ORANGE, PEAR, KAURI, ZEBRANO;
    public static final ForestryWoodType[] VALUES = values();
    public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
}
