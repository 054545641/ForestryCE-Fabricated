package forestry.apiculture;

/** Existing gameplay defaults, independent of the loader entrypoint. */
public final class ApicultureSettings {
    public static boolean hivesDamageOnPeaceful = false;
    public static boolean hivesDamageUnderwater = true;
    public static boolean hivesDamageOnlyPlayers = false;
    public static boolean hiveDamageOnAttack = true;
    public static int maxFlowersSpawnedPerHive = 20;
    public static double getSecondPrincessChance() { return 0; }
    private ApicultureSettings() {}
}
