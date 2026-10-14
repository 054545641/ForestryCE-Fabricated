package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ForestryPressurePlateBlock extends PressurePlateBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryPressurePlateBlock(Properties properties, ForestryWoodType type) {
		super(type.getBlockSetType(), properties.mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(0.5f).ignitedByLava().pushReaction(PushReaction.DESTROY));

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.PRESSURE_PLATE;
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
