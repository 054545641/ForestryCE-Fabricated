package forestry.arboriculture.models;

import com.google.common.base.Preconditions;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.arboriculture.leaves.AbstractLeavesBlock;
import forestry.arboriculture.leaves.DefaultLeavesBlock;
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

import java.util.Objects;

public class DefaultLeavesModel extends ModelBlockCached<DefaultLeavesBlock, DefaultLeavesModel.Key> {
	public DefaultLeavesModel() {
		super(DefaultLeavesBlock.class);
	}

	public static final class Key {
		public final Identifier speciesId;
		public final boolean fancy;
		private final int hashCode;

		public Key(Identifier speciesId, boolean fancy) {
			this.speciesId = speciesId;
			this.fancy = fancy;
			this.hashCode = Objects.hash(speciesId, fancy);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof DefaultLeavesModel.Key otherKey)) {
				return false;
			} else {
				// species IDs are passed around so == is fine
				return otherKey.speciesId.equals(this.speciesId) && otherKey.fancy == this.fancy;
			}
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}
	}

	@Override
	protected DefaultLeavesModel.Key getInventoryKey(ItemStack stack) {
		Block block = Block.byItem(stack.getItem());
		Preconditions.checkArgument(block instanceof DefaultLeavesBlock, "ItemStack must be for default leaves.");
		DefaultLeavesBlock bBlock = (DefaultLeavesBlock) block;
		return new Key(bBlock.getSpeciesId(), Minecraft.getInstance().options.cutoutLeaves().get());
	}

	@Override
	protected DefaultLeavesModel.Key getWorldKey(BlockState state, ModelData extraData) {
		if (state.getBlock() instanceof DefaultLeavesBlock block) {
			Identifier treeDefinition = block.getSpeciesId();
			return new DefaultLeavesModel.Key(treeDefinition, Minecraft.getInstance().options.cutoutLeaves().get());
		} else {
			throw new IllegalArgumentException("state must be for default leaves.");
		}
	}

	@Override
	protected void bakeBlock(DefaultLeavesBlock block, ModelData extraData, Key key, ModelBaker baker, boolean inventory) {
		Identifier speciesId = key.speciesId;

		// Resolve fail-soft: a datapack may have removed this species while its leaf block is still placed, so use
		// getSpeciesSafe (not the throwing getTreeSpecies) and fall back to the default species.
		ITreeSpecies species = SpeciesUtil.TREE_TYPE.get().getSpeciesSafe(speciesId);
		if (species == null) {
			species = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies();
		}
		ILeafSprite leafSpriteProvider = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(species);
		if (leafSpriteProvider == null) {
			species = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies();
			leafSpriteProvider = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(species);
		}

		Identifier leafSpriteLocation = leafSpriteProvider.get(false, key.fancy);
		TextureAtlasSprite leafSprite = getSprite(leafSpriteLocation);

		// Render the plain leaf block.
		baker.addBlockModel(leafSprite, AbstractLeavesBlock.FOLIAGE_COLOR_INDEX);

		// Set the particle sprite
		baker.setParticleSprite(leafSprite);
	}



}
