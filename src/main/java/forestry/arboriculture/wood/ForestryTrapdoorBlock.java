package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import forestry.core.platform.util.BlockUtil;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class ForestryTrapdoorBlock extends TrapDoorBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryTrapdoorBlock(Properties properties, ForestryWoodType type) {
		super(type.getBlockSetType(), properties.mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(3f).noOcclusion().isValidSpawn(BlockUtil.NEVER_SPAWN).ignitedByLava());

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.TRAPDOOR;
	}

	@Override
	public boolean isFireproof() {
		return false;
	}

	@Override
	public IWoodType getWoodType() {
		return this.type;
	}
}
