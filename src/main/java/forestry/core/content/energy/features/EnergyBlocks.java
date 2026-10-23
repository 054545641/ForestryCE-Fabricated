package forestry.core.content.energy.features;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.item.ItemBlockForestry;
import forestry.core.platform.item.ItemBlockTesr;
import forestry.core.content.energy.blocks.EngineBlock;
import forestry.core.content.energy.blocks.EngineBlockType;
import forestry.core.content.energy.blocks.SolarPanelBlock;
import forestry.core.platform.registration.FeatureBlock;
import forestry.core.platform.registration.FeatureBlockGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.List;

@FeatureProvider
public class EnergyBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(ForestryModuleIds.ENERGY);

	public static final FeatureBlockGroup<EngineBlock, EngineBlockType> ENGINES = REGISTRY.blockGroup(EngineBlock::new, List.of(EngineBlockType.VALUES)).item(ItemBlockTesr::new).identifier("engine", forestry.core.platform.registration.FeatureGroup.IdentifierType.SUFFIX).create();

	// Deviation from 1.20.1: the block factory now takes the Properties as an argument and the
	// properties come from a separate supplier, and Properties.copy was renamed to ofFullCopy.
	public static final FeatureBlock<Block, BlockItem> SOLAR_PANEL = REGISTRY.block(SolarPanelBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DAYLIGHT_DETECTOR).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK), ItemBlockForestry::new, "solar_panel");
}
