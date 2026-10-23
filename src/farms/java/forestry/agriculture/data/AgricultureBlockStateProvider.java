package forestry.agriculture.data;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import forestry.core.data.models.BlockAssetsProvider;
import forestry.core.data.models.ModelAsset;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;

import forestry.agriculture.features.MinifarmBlocks;
import forestry.agriculture.features.MultifarmBlocks;
import forestry.agriculture.multifarm.blocks.MultifarmBlockType;
import forestry.agriculture.multifarm.blocks.MultifarmMaterialType;
import forestry.agriculture.multifarm.blocks.MultifarmBlock;
import forestry.agriculture.minifarm.blocks.MinifarmBlockType;
import forestry.api.ForestryConstants;
import forestry.core.data.models.ForestryBlockStateProvider;

import static forestry.core.data.models.ForestryBlockStateProvider.file;
import static forestry.core.data.models.ForestryBlockStateProvider.mcBlock;
import static forestry.core.data.models.ForestryBlockStateProvider.modBlock;
import static forestry.core.data.models.ForestryBlockStateProvider.path;

/**
 * Generates the blockstates, block models and item models for the farms jar. A farm block is a pillar
 * of its material carrying a farm overlay, and a planter renders one hand-authored model per planter
 * type.
 */
public class AgricultureBlockStateProvider extends BlockAssetsProvider {
	public AgricultureBlockStateProvider(PackOutput output) {
		super(output);
	}

	@Override
	protected void registerStatesAndModels() {
		for (MultifarmBlock block : MultifarmBlocks.FARM.getBlocks()) {
			if (block.getType() == MultifarmBlockType.PLAIN) {
				plainFarm(block);
			} else {
				singleFarm(block);
			}

			ForestryBlockStateProvider.generic3d(this, block);
		}

		for (MinifarmBlockType planter : MinifarmBlockType.values()) {
			ModelAsset model = models().getExistingFile(modBlock(this, planter.getSerializedName()));
			Block managed = MinifarmBlocks.MANAGED_PLANTER.get(planter).block();
			Block manual = MinifarmBlocks.MANUAL_PLANTER.get(planter).block();

			ForestryBlockStateProvider.horizontalForestryBlock(this, managed, model);
			ForestryBlockStateProvider.horizontalForestryBlock(this, manual, model);

			planterItem(managed, planter);
			planterItem(manual, planter);
		}
	}

	private void singleFarm(MultifarmBlock block) {
		MultifarmMaterialType material = block.getFarmMaterial();
		Block base = material.getBase();
		Identifier texture = modBlock(this, "farm/" + block.getType().getSerializedName());

		ForestryBlockStateProvider.singleModelBlock(this, block, farmPillar(path(block), base, texture, texture));
	}

	private void plainFarm(MultifarmBlock block) {
		MultifarmMaterialType material = block.getFarmMaterial();
		Block base = material.getBase();

		// todo need to use reverse texture
		getVariantBuilder(block)
			.partialState().with(MultifarmBlock.BAND, false)
			.modelForState().modelFile(farmPillar(path(block), base, modBlock(this, "farm/top"), modBlock(this, "farm/plain"))).addModel()
			.partialState().with(MultifarmBlock.BAND, true)
			.modelForState().modelFile(farmPillar(path(block) + "_band", base, modBlock(this, "farm/top"), modBlock(this, "farm/band"))).addModel();
	}

	private ModelAsset farmPillar(String path, Block base, Identifier top, Identifier side) {
		ModelAsset baseModel = file(blockTexture(base));

		ModelAsset overlay = models().withExistingParent(path + "_overlay", mcLoc("block/cube_column"))
			.texture("end", top).texture("side", side).renderType("cutout");
		ModelAsset result = models().getBuilder(path).parent(baseModel);
		new CompositeModelBuilder().child("base", baseModel.getLocation()).child("overlay", overlay.getLocation())
			.itemRenderOrder("base", "overlay").toJson(result.json);
		return result;
	}

	// Every planter type shares one hand-authored block model, and its item renders that model
	private void planterItem(Block block, MinifarmBlockType planter) {
		itemModels().withExistingParent(path(block), modBlock(this, planter.getSerializedName()));
	}
}
