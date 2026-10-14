package forestry.arboriculture.models;

import com.google.common.base.Preconditions;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.IFruit;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.arboriculture.leaves.AbstractLeavesBlock;
import forestry.arboriculture.leaves.DecorativeLeavesBlock;
import forestry.core.platform.models.ModelBlockCached;
import forestry.core.platform.models.ModelTransforms;
import forestry.core.platform.models.baker.ModelBaker;
import forestry.core.platform.models.baker.ModelBakerModel;
import forestry.core.platform.util.ResourceUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

public class DecorativeLeavesModel<B extends Block> extends ModelBlockCached<B, DefaultLeavesModel.Key> {
	public DecorativeLeavesModel(Class<B> blockClass) {
		super(blockClass);
	}

	@Override
	protected DefaultLeavesModel.Key getInventoryKey(ItemStack stack) {
		Block block = Block.byItem(stack.getItem());
		Preconditions.checkArgument(block instanceof DecorativeLeavesBlock, "ItemStack must be for decorative leaves.");
		DecorativeLeavesBlock bBlock = (DecorativeLeavesBlock) block;
		return new DefaultLeavesModel.Key(bBlock.getSpeciesId(), Minecraft.getInstance().options.cutoutLeaves().get());
	}

	@Override
	protected DefaultLeavesModel.Key getWorldKey(BlockState state, ModelData extraData) {
		Block block = state.getBlock();
		Preconditions.checkArgument(block instanceof DecorativeLeavesBlock, "state must be for decorative leaves.");
		DecorativeLeavesBlock bBlock = (DecorativeLeavesBlock) block;
		return new DefaultLeavesModel.Key(bBlock.getSpeciesId(), Minecraft.getInstance().options.cutoutLeaves().get());
	}

	@Override
	protected void bakeBlock(B block, ModelData extraData, DefaultLeavesModel.Key key, ModelBaker baker, boolean inventory) {
		Identifier speciesId = key.speciesId;

		// Resolve fail-soft: a datapack may have removed this species while its leaf block is still placed, so use
		// getSpeciesSafe (not the throwing getTreeSpecies) and fall the species itself back to default - it's reused
		// for the fruit-overlay lookup below, so it must be non-null.
		ITreeSpecies species = SpeciesUtil.TREE_TYPE.get().getSpeciesSafe(speciesId);
		if (species == null) {
			species = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies();
		}
		ILeafSprite sprite = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(species);
		if (sprite == null) {
			// species exists but registered no client sprite: keep it as-is (used below for the fruit overlay
			// lookup); only the sprite falls back, so an unregistered-client species doesn't NPE the leaf render.
			sprite = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(SpeciesUtil.TREE_TYPE.get().getDefaultSpecies());
		}

		Identifier textureLocation = sprite.get(false, key.fancy);
		TextureAtlasSprite textureSprite = getSprite(textureLocation);
		// Render the plain leaf block.
		baker.addBlockModel(textureSprite, AbstractLeavesBlock.FOLIAGE_COLOR_INDEX);

		// Render overlay for fruit leaves.
		IFruit fruit = species.getDefaultGenome().resolveActive(TreeChromosomes.FRUIT);
		Identifier fruitSpriteLocation = fruit.getDecorativeSprite();
		if (fruitSpriteLocation != null) {
			TextureAtlasSprite fruitSprite = getSprite(fruitSpriteLocation);
			baker.addBlockModel(fruitSprite, AbstractLeavesBlock.FRUIT_COLOR_INDEX);
		}

		// Set the particle sprite
		Identifier particleLocation = sprite.getParticle();
		TextureAtlasSprite particleSprite = getSprite(particleLocation);
		baker.setParticleSprite(particleSprite);
	}


}
