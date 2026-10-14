package forestry.arboriculture.wood;

import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class ForestryHangingSignBlock extends CeilingHangingSignBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryHangingSignBlock(Properties properties, ForestryWoodType type) {
		super(type.getWoodType(), properties.mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1f).ignitedByLava());

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.HANGING_SIGN;
	}

	@Override
	public boolean isFireproof() {
		return false;
	}

	@Override
	public ForestryWoodType getWoodType() {
		return this.type;
	}

	// newBlockEntity / getTicker are inherited from CeilingHangingSignBlock and use vanilla
	// BlockEntityType.HANGING_SIGN. Forestry registers this block as a valid block for that
	// BE type via BlockEntityTypeAddBlocksEvent in ModuleArboriculture.
}
