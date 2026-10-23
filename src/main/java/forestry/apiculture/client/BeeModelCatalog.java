package forestry.apiculture.client;

import forestry.api.ForestryConstants;
import forestry.api.apiculture.ForestryBeeSpecies;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.ILifeStage;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Model declarations are independent of the server's currently loaded species and the hive particle renderer. */
public final class BeeModelCatalog {
    public static final BeeModelCatalog INSTANCE = new BeeModelCatalog();
    private final Map<ILifeStage, Identifier> defaults = new IdentityHashMap<>();
    private final Map<ILifeStage, Map<Identifier, Identifier>> custom = new IdentityHashMap<>();
    private BeeModelCatalog() {
        for (var stage : BeeLifeStage.values()) {
            defaults.put(stage, ForestryConstants.forestry("item/bee_" + stage.getSerializedName() + "_default"));
            if (stage != BeeLifeStage.LARVAE) custom.put(stage, Map.of(ForestryBeeSpecies.VANILLA, ForestryConstants.forestry("item/bee_" + stage.getSerializedName() + "_cube")));
        }
    }
    public void replace(Map<ILifeStage, Identifier> defaults, Map<ILifeStage, Map<Identifier, Identifier>> custom) {
        this.defaults.clear(); this.defaults.putAll(defaults);
        this.custom.clear(); custom.forEach((stage, models) -> this.custom.put(stage, Map.copyOf(models)));
    }
    public Identifier getDefaultModelLocation(ILifeStage stage) { return Objects.requireNonNull(defaults.get(stage)); }
    public Identifier getModelLocation(ILifeStage stage, Identifier species) { return custom.getOrDefault(stage, Map.of()).getOrDefault(species, getDefaultModelLocation(stage)); }
    public Set<Identifier> getAllModelLocations(ILifeStage stage) {
        var result = new LinkedHashSet<Identifier>(custom.getOrDefault(stage, Map.of()).values()); result.add(getDefaultModelLocation(stage)); return result;
    }
}
