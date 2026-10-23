package forestry.lepidopterology.client;

import com.mojang.datafixers.util.Pair;
import forestry.api.lepidopterology.ForestryButterflySpecies;
import forestry.api.lepidopterology.genetics.IButterflySpecies;
import forestry.lepidopterology.butterflies.ButterflyTextures;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class ButterflyTextureCatalog {
    public static final ButterflyTextureCatalog INSTANCE = new ButterflyTextureCatalog();
    private static Pair<Identifier, Identifier> textures(Identifier species) { return Pair.of(ButterflyTextures.item(species), ButterflyTextures.entity(species)); }
    public Collection<Pair<Identifier, Identifier>> getAllTextures() { return ForestryButterflySpecies.ALL.stream().map(ButterflyTextureCatalog::textures).toList(); }
    public Pair<Identifier, Identifier> getTextures(IButterflySpecies species) { return textures(species.id()); }
    public Pair<Identifier, Identifier> getDefaultTextures() { return textures(ForestryButterflySpecies.CABBAGE_WHITE); }
}
