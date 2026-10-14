package forestry.arboriculture.models;

import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.client.IForestryClientApi;
import forestry.api.client.arboriculture.ILeafSprite;
import forestry.arboriculture.leaves.AbstractLeavesBlock;
import forestry.arboriculture.leaves.BlockForestryLeaves;
import forestry.arboriculture.features.ArboricultureBlocks;
import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.core.platform.models.ModelBlockCached;
import forestry.core.platform.models.ModelTransforms;
import forestry.core.platform.models.baker.ModelBaker;
import forestry.core.platform.util.NBTUtilForestry;
import forestry.core.platform.util.ResourceUtil;
import forestry.core.platform.util.SpeciesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.Objects;

public class LeavesModel extends ModelBlockCached<BlockForestryLeaves, LeavesModel.Key> {

	public LeavesModel() {
		super(BlockForestryLeaves.class);
	}

	public static class Key {
		public final TextureAtlasSprite leafSprite;
		@Nullable
		public final TextureAtlasSprite fruitSprite;
		public final boolean fancy;
		private final int hashCode;

		public Key(TextureAtlasSprite leafSprite, @Nullable TextureAtlasSprite fruitSprite, boolean fancy) {
			this.leafSprite = leafSprite;
			this.fruitSprite = fruitSprite;
			this.fancy = fancy;
			this.hashCode = Objects.hash(leafSprite, fruitSprite, fancy);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof Key otherKey)) {
				return false;
			} else {
				return otherKey.leafSprite == this.leafSprite && otherKey.fruitSprite == this.fruitSprite && otherKey.fancy == this.fancy;
			}
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}
	}

	@Override
	protected Key getInventoryKey(ItemStack stack) {
		LeavesBlockEntity leaves = new LeavesBlockEntity(BlockPos.ZERO, ArboricultureBlocks.LEAVES.defaultState());
		CompoundTag tag = NBTUtilForestry.getItemStackTag(stack);
		if (tag != null) {
			forestry.core.platform.util.ValueIOUtil.readTag(tag, net.minecraft.core.RegistryAccess.EMPTY, leaves::loadCustomOnly);
		} else {
			leaves.setTree(SpeciesUtil.TREE_TYPE.get().getDefaultSpecies().createIndividual());
		}
		return getKey(leaves.getModelData());
	}

	@Override
	protected Key getWorldKey(BlockState state, ModelData extraData) {
		return getKey(extraData);
	}

	private Key getKey(ModelData extraData) {
		boolean fancy = Minecraft.getInstance().options.cutoutLeaves().get();

		ITreeSpecies species = extraData.get(LeavesBlockEntity.PROPERTY_SPECIES);
		if (species == null) {
			species = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies();
		}
		ILeafSprite sprite = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(species);
		if (sprite == null) {
			species = SpeciesUtil.TREE_TYPE.get().getDefaultSpecies();
			sprite = IForestryClientApi.INSTANCE.getTreeManager().getLeafSprite(species);
		}
		Identifier leafLocation = sprite.get(Boolean.TRUE.equals(extraData.get(LeavesBlockEntity.PROPERTY_POLLINATED)), fancy);
		Identifier fruitLocation = extraData.get(LeavesBlockEntity.PROPERTY_FRUIT_TEXTURE);

		return new Key(getSprite(leafLocation), fruitLocation != null ? getSprite(fruitLocation) : null, fancy);
	}

	@Override
	protected void bakeBlock(BlockForestryLeaves block, ModelData extraData, Key key, ModelBaker baker, boolean inventory) {
		// Render the plain leaf block.
		baker.addBlockModel(key.leafSprite, AbstractLeavesBlock.FOLIAGE_COLOR_INDEX);

		if (key.fruitSprite != null) {
			baker.addBlockModel(key.fruitSprite, AbstractLeavesBlock.FRUIT_COLOR_INDEX);
		}

		// Set the particle sprite
		baker.setParticleSprite(key.leafSprite);
	}


}
