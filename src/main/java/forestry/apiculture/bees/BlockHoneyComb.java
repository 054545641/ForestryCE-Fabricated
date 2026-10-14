package forestry.apiculture.bees;

import forestry.apiculture.bees.EnumHoneyComb;
import forestry.core.platform.block.IColoredBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BlockHoneyComb extends Block implements IColoredBlock {
	public final EnumHoneyComb type;

	public BlockHoneyComb(Properties properties, EnumHoneyComb type) {
		super(properties.sound(SoundType.CORAL_BLOCK).strength(1F));
		this.type = type;
	}

	public EnumHoneyComb getType() {
		return this.type;
	}

	@Override
	public int colorMultiplier(BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int tintIndex) {
		EnumHoneyComb honeyComb = this.type;
		if (tintIndex == 1) {
			return honeyComb.primaryColor;
		} else {
			return honeyComb.secondaryColor;
		}
	}
}
