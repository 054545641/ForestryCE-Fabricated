package forestry.apiculture.data;
import forestry.apiculture.bees.genetics.*;
import forestry.apiculture.plugin.DefaultBeeSpecies;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.resources.Identifier;
import java.util.Map;
/** Built-in datagen input, separate from the runtime species provider API. */
public final class BuiltinBeeSpeciesData {
    public static Map<Identifier, BeeSpeciesDefinition> buildDefinitions() {
        return BeeSpeciesProvider.buildDefinitions(SpeciesUtil.BEE_TYPE.get(), DefaultBeeSpecies::register);
    }
    public static void seedLiveSpeciesForDatagen() { ApicultureReloadHandler.rebuildSpecies(buildDefinitions()); }
}
