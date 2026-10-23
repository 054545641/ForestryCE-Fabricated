package forestry.core.data;

import forestry.core.data.models.ItemAssetsProvider;
import forestry.core.data.models.ModelAssets;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import thedarkcolour.modkit.data.DataHelper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Gives ModKit providers a per-jar output root, then attaches them to the shared hash cache. */
public final class ScopedDataHelper extends DataHelper {
	private final GatherDataEvent parent;
	private final String jar;
	public ScopedDataHelper(String modId, GatherDataEvent parent, PackOutput output, String jar) {
		super(modId, new ScopeEvent(parent, output));
		this.parent=parent;
		this.jar=jar;
	}
	public void createForestryItemModels(Consumer<ModelAssets> register) {
		this.event.getGenerator().addProvider(true,new ItemAssetsProvider(this.event.getGenerator().getPackOutput(),register));
	}
	@FunctionalInterface
	public interface Recipes {
		void generate(net.minecraft.data.recipes.RecipeOutput output, thedarkcolour.modkit.data.MKRecipeProvider recipes, HolderLookup.Provider registries);
	}
	public void createForestryRecipes(Recipes register) {
		createRecipes((output, recipes) -> register.generate(output, recipes, this.event.getLookupProvider().join()));
	}
	public void attach() {
		this.event.getGenerator().getProvidersView().values().forEach(provider ->
			this.parent.getGenerator().addProvider(true,new RenamedProvider(this.jar+"/"+provider.getName(),provider)));
	}
	private static final class ScopeEvent extends GatherDataEvent.Client {
		private final GatherDataEvent parent;
		ScopeEvent(GatherDataEvent parent, PackOutput output) {
			super(parent.getModContainer(),new DataGenerator.Uncached(output.getOutputFolder()),null);
			this.parent=parent;
		}
		@Override public CompletableFuture<HolderLookup.Provider> getLookupProvider() { return this.parent.getLookupProvider(); }
		@Override public ResourceManager getResourceManager(PackType type) { return this.parent.getResourceManager(type); }
	}
}
