package forestry.arboriculture.wood;

import forestry.api.arboriculture.IWoodType;
import forestry.api.arboriculture.WoodBlockKind;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.material.PushReaction;

public class ForestryButtonBlock extends ButtonBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryButtonBlock(Properties properties, ForestryWoodType type) {
		super(type.getBlockSetType(), 30, properties.noCollision().strength(0.5f).pushReaction(PushReaction.DESTROY));

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.BUTTON;
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
