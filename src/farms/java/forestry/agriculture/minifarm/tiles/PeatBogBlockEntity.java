package forestry.agriculture.minifarm.tiles;

import forestry.api.agriculture.ForestryFarmTypes;
import forestry.core.features.CoreTerrain;

import forestry.agriculture.features.MinifarmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PeatBogBlockEntity extends AbstractMinifarmBlockEntity {
	public PeatBogBlockEntity(BlockPos pos, BlockState state) {
		super(MinifarmBlockEntities.BOG.tileType(), pos, state, ForestryFarmTypes.PEAT);
	}

	@Override
	public List<ItemStack> createGermlingStacks() {
		return List.of();
	}

	@Override
	public List<ItemStack> createResourceStacks() {
		return List.of(
			CoreTerrain.BOG_EARTH.stack(),
			CoreTerrain.BOG_EARTH.stack(),
			CoreTerrain.BOG_EARTH.stack(),
			CoreTerrain.BOG_EARTH.stack()
		);
	}

	@Override
	public List<ItemStack> createProductionStacks() {
		return List.of(
			new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("peat"))),
			new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("peat"))),
			new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("peat"))),
			new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(forestry.api.ForestryConstants.forestry("peat")))
		);
	}
}
