package forestry.arboriculture.charcoal;

import com.google.common.base.Preconditions;
import forestry.api.arboriculture.ICharcoalManager;
import forestry.api.arboriculture.ICharcoalPileWall;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CharcoalManager implements ICharcoalManager {
	public static final int charcoalAmountBase = 8;
	public static final int charcoalWallCheckRange = 16;
	private final List<ICharcoalPileWall> walls = new ArrayList<>();

	/**
	 * Internal-only adder used by {@code ArboricultureRegistration.registerCharcoalPitWall}.
	 * Plugins must register through {@link forestry.api.plugin.IArboricultureRegistration#registerCharcoalPitWall}.
	 */
	public void addWall(BlockState blockState, int amount) {
		Preconditions.checkNotNull(blockState, "block state must not be null.");
		int minExclusive = -charcoalAmountBase;
		int maxExclusive = 63 - charcoalAmountBase;
		Preconditions.checkArgument(amount > minExclusive && amount < maxExclusive,
				"amount must be greater than %s and less than %s (charcoalAmountBase=%s).",
				minExclusive, maxExclusive, charcoalAmountBase);
		this.walls.add(new CharcoalPileWall(blockState, amount));
	}

	@Nullable
	@Override
	public ICharcoalPileWall getWall(BlockState state) {
		for (ICharcoalPileWall wall : this.walls) {
			if (wall.matches(state)) {
				return wall;
			}
		}
		return null;
	}

	@Override
	public List<ICharcoalPileWall> getWalls() {
		// Read-only view: callers must register through IArboricultureRegistration.registerCharcoalPitWall
		// rather than mutating the list returned by this getter.
		return Collections.unmodifiableList(this.walls);
	}
}
