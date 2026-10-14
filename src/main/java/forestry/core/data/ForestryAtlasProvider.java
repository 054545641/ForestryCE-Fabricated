package forestry.core.data;

import forestry.api.ForestryConstants;
import forestry.core.platform.render.ForestrySpriteUploader;
import net.minecraft.client.renderer.texture.atlas.sources.DirectoryLister;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.Identifier;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.core.data.models.ForestryWoodModelProvider;
import forestry.core.platform.util.SpeciesUtil;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.data.SpriteSourceProvider;
import java.util.concurrent.CompletableFuture;

public class ForestryAtlasProvider extends SpriteSourceProvider {
	public ForestryAtlasProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(output, lookupProvider, ForestryConstants.MOD_ID);
	}

	@Override
	protected void gather() {
		SourceList icons = atlas(ForestrySpriteUploader.ATLAS_PATH);

		icons.addSource(new DirectoryLister("forestry/atlas/gui", ""));

		// Block and item atlases are separate in 26.2. Saplings reuse the PNG but require a block
		// sprite ID, otherwise SimpleModelWrapper rejects the entire crossed block model.
		SourceList blocks = atlas(Identifier.withDefaultNamespace("blocks"));
		for (var species : SpeciesUtil.getAllTreeSpecies()) {
			if (ForestryItemModels.VANILLA_SAPLINGS.contains(species.id())) continue;
			String name = species.id().getPath() + "_sapling";
			blocks.addSource(new SingleFile(ForestryConstants.forestry("item/" + name), Optional.of(ForestryConstants.forestry("block/saplings/" + name))));
		}
		for (ForestryWoodType wood : ForestryWoodType.VALUES) {
			String texture = ForestryWoodModelProvider.legacyTextureName(wood);
			blocks.addSource(new SingleFile(ForestryConstants.forestry("entity/signs/" + texture)));
			blocks.addSource(new SingleFile(ForestryConstants.forestry("entity/signs/hanging/" + texture)));
		}
	}
}
