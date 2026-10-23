package forestry.core.data.models;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ItemLike;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ItemAssetsProvider implements DataProvider {
	private final PackOutput output;
	private final Consumer<ModelAssets> register;
	private final Map<Identifier, ModelAsset> models = new LinkedHashMap<>();
	protected final ModelAssets assets = new ModelAssets(this.models,"item");
	public ItemAssetsProvider(PackOutput output) { this(output, unused -> {}); }
	public ItemAssetsProvider(PackOutput output, Consumer<ModelAssets> register) { this.output=output; this.register=register; }
	protected void registerModels() { this.register.accept(this.assets); }
	public Identifier modLoc(String path) { return this.assets.modLoc(path); }
	public Identifier mcLoc(String path) { return this.assets.mcLoc(path); }
	public ModelAsset getBuilder(String path) { return this.assets.getBuilder(path); }
	public ModelAsset withExistingParent(String path,Identifier parent) { return this.assets.withExistingParent(path,parent); }
	public void basicItem(ItemLike item) { this.assets.generic2d(item); }
	@Override public CompletableFuture<?> run(CachedOutput cache) {
		this.models.clear(); registerModels();
		var paths=this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK,"models");
		return CompletableFuture.allOf(this.models.entrySet().stream().map(e->DataProvider.saveStable(cache,e.getValue().json,paths.json(e.getKey()))).toArray(CompletableFuture[]::new));
	}
	@Override public String getName() { return getClass().getSimpleName(); }
}
