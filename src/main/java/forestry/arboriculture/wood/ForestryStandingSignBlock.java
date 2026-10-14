package forestry.arboriculture.wood;

import forestry.api.arboriculture.WoodBlockKind;
import forestry.arboriculture.features.ArboricultureTiles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import javax.annotation.Nullable;

public class ForestryStandingSignBlock extends StandingSignBlock implements IWoodTyped {
	private final ForestryWoodType type;

	public ForestryStandingSignBlock(Properties properties, ForestryWoodType type) {
		super(type.getWoodType(), properties.mapColor(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollision().strength(1.0F).ignitedByLava());

		this.type = type;
	}

	@Override
	public WoodBlockKind getBlockKind() {
		return WoodBlockKind.SIGN;
	}

	@Override
	public boolean isFireproof() {
		return false;
	}

	@Override
	public ForestryWoodType getWoodType() {
		return this.type;
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> actual) {
		return createTickerHelper(actual, ArboricultureTiles.SIGN.tileType(), SignBlockEntity::tick);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SignBlockEntity(ArboricultureTiles.SIGN.tileType(), pos, state);
	}
}
