package forestry.lepidopterology.butterflies;

import net.minecraft.resources.Identifier;

/** Stable resource naming is shared by entity state and the native item renderer. */
public final class ButterflyTextures {
    public static Identifier item(Identifier species) { return Identifier.fromNamespaceAndPath(species.getNamespace(), "item/butterfly/" + species.getPath().replace("butterfly_", "")); }
    public static Identifier entity(Identifier species) { return Identifier.fromNamespaceAndPath(species.getNamespace(), "textures/entity/butterfly/" + species.getPath().replace("butterfly_", "") + ".png"); }
}
