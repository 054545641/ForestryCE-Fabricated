package forestry.apiculture.bees;
public enum BeeHousingIcon {
 APIARY("/apiary.png"), BEE_HOUSE("/alveary.png");
 public static final BeeHousingIcon[] VALUES = values();
 public final String path;
 BeeHousingIcon(String path) { this.path = path; }
}
